package com.aaryo.vedabrowser.download

import android.net.Uri
import android.webkit.URLUtil
import com.aaryo.vedabrowser.model.SniffedMediaItem
import com.aaryo.vedabrowser.model.SniffedMediaType
import com.aaryo.vedabrowser.util.FilenameSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Chromium Media Sniffer & Video/Audio Detector.
 *
 * Intercepts network media requests (MP4, WebM, M3U8 HLS, MP3, M4A, etc.) and
 * hooks Blink's HTMLMediaElement runtime to detect playable video/audio streams on web pages.
 */
object MediaSnifferEngine {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _mediaByTab = MutableStateFlow<Map<String, List<SniffedMediaItem>>>(emptyMap())
    val mediaByTab: StateFlow<Map<String, List<SniffedMediaItem>>> = _mediaByTab.asStateFlow()

    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "flv", "3gp", "avi", "ts")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "weba")
    private val STREAM_EXTENSIONS = setOf("m3u8", "mpd")

    private val IGNORED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "svg", "ico", "bmp",
        "css", "js", "json", "html", "htm", "xml", "txt", "woff", "woff2", "ttf", "eot"
    )

    private val MEDIA_REGEX = Pattern.compile(
        ".*\\.(mp4|webm|mkv|mov|flv|3gp|avi|ts|mp3|m4a|aac|wav|ogg|flac|opus|weba|m3u8|mpd)(\\?.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    fun getMediaForTab(tabId: String): List<SniffedMediaItem> {
        return _mediaByTab.value[tabId] ?: emptyList()
    }

    fun getMediaCount(tabId: String): Int {
        return _mediaByTab.value[tabId]?.size ?: 0
    }

    fun clearTabMedia(tabId: String) {
        val current = _mediaByTab.value.toMutableMap()
        current.remove(tabId)
        _mediaByTab.value = current
    }

    /**
     * Determines whether an HTTP request or URL is a media file.
     */
    fun isMediaUrl(url: String?, headers: Map<String, String>? = null): Boolean {
        if (url.isNullOrBlank()) return false
        val cleanUrl = url.trim().lowercase(Locale.ROOT)

        if (cleanUrl.startsWith("data:") || cleanUrl.startsWith("javascript:")) return false

        // Check accept headers if explicitly media
        headers?.let { h ->
            val accept = (h["Accept"] ?: h["accept"])?.lowercase(Locale.ROOT)
            if (accept != null && (accept.contains("video/") || accept.contains("audio/"))) {
                return true
            }
        }

        // Check if extension is explicitly non-media
        val ext = extractExtension(cleanUrl)
        if (ext in IGNORED_EXTENSIONS) return false

        if (ext in VIDEO_EXTENSIONS || ext in AUDIO_EXTENSIONS || ext in STREAM_EXTENSIONS) {
            return true
        }

        // Common media paths
        if (cleanUrl.contains("/videoplayback") ||
            cleanUrl.contains(".m3u8") ||
            cleanUrl.contains("/hls/") ||
            cleanUrl.contains("/dash/") ||
            cleanUrl.contains("mime=video") ||
            cleanUrl.contains("mime=audio")
        ) {
            return true
        }

        return MEDIA_REGEX.matcher(cleanUrl).matches()
    }

    /**
     * Registers a detected media item for a given tab. Deduplicates identical URLs.
     */
    fun onMediaDetected(
        tabId: String,
        url: String,
        pageUrl: String,
        pageTitle: String,
        mimeType: String? = null,
        mediaTypeHint: SniffedMediaType? = null,
        qualityHint: String? = null
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || cleanUrl.startsWith("data:") || cleanUrl.startsWith("javascript:")) {
            return
        }

        val normalizedUrl = cleanUrl.substringBefore('#')
        val currentList = _mediaByTab.value[tabId] ?: emptyList()

        // Deduplicate: avoid re-adding existing URL
        if (currentList.any { it.url.equals(normalizedUrl, ignoreCase = true) }) {
            return
        }

        val detectedType = mediaTypeHint ?: detectMediaType(normalizedUrl, mimeType)
        val ext = extractExtension(normalizedUrl).ifEmpty {
            when (detectedType) {
                SniffedMediaType.AUDIO -> "mp3"
                SniffedMediaType.STREAM -> "m3u8"
                SniffedMediaType.VIDEO -> "mp4"
            }
        }

        val rawFileName = deriveFileName(normalizedUrl, pageTitle, ext)
        val sanitizedFileName = FilenameSanitizer.sanitize(rawFileName)
        val qualityOrFormat = qualityHint ?: detectQualityOrFormat(normalizedUrl, detectedType, ext)
        val resolvedMime = mimeType ?: resolveMimeType(detectedType, ext)

        val item = SniffedMediaItem(
            tabId = tabId,
            url = normalizedUrl,
            pageUrl = pageUrl,
            pageTitle = pageTitle,
            filename = sanitizedFileName,
            mimeType = resolvedMime,
            mediaType = detectedType,
            sizeBytes = -1L,
            qualityOrFormat = qualityOrFormat
        )

        val updatedList = currentList + item
        val map = _mediaByTab.value.toMutableMap()
        map[tabId] = updatedList
        _mediaByTab.value = map

        // Probe size asynchronously in background for direct HTTP/HTTPS files
        if (normalizedUrl.startsWith("http://", ignoreCase = true) ||
            normalizedUrl.startsWith("https://", ignoreCase = true)
        ) {
            probeMediaSizeAsync(tabId, item.id, normalizedUrl, pageUrl)
        }
    }

    private fun detectMediaType(url: String, mimeType: String?): SniffedMediaType {
        val lower = url.lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT) ?: ""

        return when {
            lowerMime.contains("audio") || AUDIO_EXTENSIONS.any { lower.contains(".$it") } -> SniffedMediaType.AUDIO
            lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains("/hls/") ||
                    lowerMime.contains("mpegurl") || lowerMime.contains("dash+xml") -> SniffedMediaType.STREAM
            else -> SniffedMediaType.VIDEO
        }
    }

    private fun extractExtension(url: String): String {
        return try {
            val clean = url.substringBefore('?').substringBefore('#')
            val lastSlash = clean.lastIndexOf('/')
            val filenamePart = if (lastSlash != -1) clean.substring(lastSlash + 1) else clean
            val lastDot = filenamePart.lastIndexOf('.')
            if (lastDot != -1 && lastDot < filenamePart.length - 1) {
                val ext = filenamePart.substring(lastDot + 1).lowercase(Locale.ROOT)
                if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) {
                    return ext
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun deriveFileName(url: String, pageTitle: String, ext: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        val lastSlash = clean.lastIndexOf('/')
        val candidate = if (lastSlash != -1 && lastSlash < clean.length - 1) {
            clean.substring(lastSlash + 1)
        } else ""

        if (candidate.isNotBlank() && candidate.contains('.') && !candidate.startsWith("default.")) {
            return candidate
        }

        val cleanTitle = pageTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
        return if (cleanTitle.isNotBlank()) {
            "${cleanTitle.take(30)}.$ext"
        } else {
            "media_${System.currentTimeMillis() % 100000}.$ext"
        }
    }

    private fun detectQualityOrFormat(url: String, type: SniffedMediaType, ext: String): String {
        val lower = url.lowercase(Locale.ROOT)
        val quality = when {
            lower.contains("2160p") || lower.contains("4k") -> "4K UHD"
            lower.contains("1440p") || lower.contains("2k") -> "1440p QHD"
            lower.contains("1080p") || lower.contains("1080") -> "1080p FHD"
            lower.contains("720p") || lower.contains("720") -> "720p HD"
            lower.contains("480p") || lower.contains("480") -> "480p SD"
            lower.contains("360p") || lower.contains("360") -> "360p"
            else -> null
        }

        return when (type) {
            SniffedMediaType.AUDIO -> quality?.let { "$it • ${ext.uppercase(Locale.ROOT)}" } ?: "${ext.uppercase(Locale.ROOT)} Audio"
            SniffedMediaType.STREAM -> if (ext.equals("m3u8", ignoreCase = true)) "HLS Adaptive Stream (m3u8)" else "DASH Stream (mpd)"
            SniffedMediaType.VIDEO -> quality?.let { "$it • ${ext.uppercase(Locale.ROOT)}" } ?: "${ext.uppercase(Locale.ROOT)} Video"
        }
    }

    private fun resolveMimeType(type: SniffedMediaType, ext: String): String {
        return when (type) {
            SniffedMediaType.AUDIO -> when (ext) {
                "m4a" -> "audio/mp4"
                "aac" -> "audio/aac"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                else -> "audio/mpeg"
            }
            SniffedMediaType.STREAM -> when (ext) {
                "m3u8" -> "application/x-mpegURL"
                "mpd" -> "application/dash+xml"
                else -> "application/octet-stream"
            }
            SniffedMediaType.VIDEO -> when (ext) {
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "3gp" -> "video/3gpp"
                else -> "video/mp4"
            }
        }
    }

    private fun probeMediaSizeAsync(tabId: String, itemId: String, url: String, referer: String?) {
        scope.launch {
            try {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .head()
                if (!referer.isNullOrBlank()) {
                    reqBuilder.header("Referer", referer)
                }
                reqBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36")

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val length = response.header("Content-Length")?.toLongOrNull()
                        val contentType = response.header("Content-Type")

                        if (length != null && length > 0L) {
                            updateItemSize(tabId, itemId, length, contentType)
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun updateItemSize(tabId: String, itemId: String, sizeBytes: Long, contentType: String?) {
        val currentList = _mediaByTab.value[tabId] ?: return
        val updatedList = currentList.map { item ->
            if (item.id == itemId) {
                item.copy(
                    sizeBytes = sizeBytes,
                    mimeType = contentType?.substringBefore(';') ?: item.mimeType
                )
            } else {
                item
            }
        }
        val map = _mediaByTab.value.toMutableMap()
        map[tabId] = updatedList
        _mediaByTab.value = map
    }

    /**
     * Client-side JavaScript snippet injected at document start and page finish to detect
     * media tags (<video>, <audio>, <source>), dynamically loaded elements, and media playback.
     */
    val MEDIA_SNIFFER_SCRIPT: String = """
        (function() {
            if (window.__vedaMediaSnifferInstalled) return;
            window.__vedaMediaSnifferInstalled = true;

            var reportedUrls = {};

            function reportMedia(url, type, title, mimeType, quality) {
                if (!url || typeof url !== 'string') return;
                var trimmed = url.trim();
                if (!trimmed || trimmed.startsWith('javascript:')) return;
                if (trimmed.startsWith('data:image')) return;

                try {
                    var absoluteUrl = new URL(trimmed, document.baseURI).href;
                    if (reportedUrls[absoluteUrl]) return;
                    reportedUrls[absoluteUrl] = true;

                    var payload = JSON.stringify({
                        type: 'MEDIA_DETECTED',
                        url: absoluteUrl,
                        pageUrl: window.location.href,
                        pageTitle: document.title || '',
                        mediaType: type || 'VIDEO',
                        mimeType: mimeType || '',
                        quality: quality || ''
                    });

                    if (window.vedaBridge && typeof window.vedaBridge.postMessage === 'function') {
                        window.vedaBridge.postMessage(payload);
                    } else if (window.vedaBridgeLegacy && typeof window.vedaBridgeLegacy.postMessageLegacy === 'function') {
                        window.vedaBridgeLegacy.postMessageLegacy(payload);
                    }
                } catch(e) {}
            }

            function scanDom() {
                try {
                    var videos = document.querySelectorAll('video, audio, source, [data-video-url], [data-src]');
                    for (var i = 0; i < videos.length; i++) {
                        var el = videos[i];
                        var tag = el.tagName.toLowerCase();
                        var src = el.currentSrc || el.src || el.getAttribute('src') || el.getAttribute('data-src') || el.getAttribute('data-video-url');
                        var isAudio = tag === 'audio';
                        if (src) {
                            var mime = el.type || (isAudio ? 'audio/mpeg' : 'video/mp4');
                            reportMedia(src, isAudio ? 'AUDIO' : 'VIDEO', document.title, mime);
                        }
                    }
                } catch(e) {}
            }

            // Expose manual scan function for the browser UI
            window.vedaScanMediaNow = function() {
                scanDom();
            };

            // Hook HTMLMediaElement prototype
            try {
                var origPlay = HTMLMediaElement.prototype.play;
                HTMLMediaElement.prototype.play = function() {
                    var src = this.currentSrc || this.src;
                    var isAudio = this.tagName.toLowerCase() === 'audio';
                    if (src) {
                        reportMedia(src, isAudio ? 'AUDIO' : 'VIDEO', document.title, this.type);
                    }
                    return origPlay.apply(this, arguments);
                };

                var origLoad = HTMLMediaElement.prototype.load;
                HTMLMediaElement.prototype.load = function() {
                    var src = this.currentSrc || this.src;
                    var isAudio = this.tagName.toLowerCase() === 'audio';
                    if (src) {
                        reportMedia(src, isAudio ? 'AUDIO' : 'VIDEO', document.title, this.type);
                    }
                    return origLoad.apply(this, arguments);
                };
            } catch(e) {}

            // MutationObserver to detect dynamically added video elements
            try {
                var observer = new MutationObserver(function(mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                        var m = mutations[i];
                        if (m.type === 'childList') {
                            for (var j = 0; j < m.addedNodes.length; j++) {
                                var node = m.addedNodes[j];
                                if (node.nodeType === 1) {
                                    var tag = (node.tagName || '').toLowerCase();
                                    if (tag === 'video' || tag === 'audio') {
                                        var src = node.currentSrc || node.src;
                                        if (src) reportMedia(src, tag === 'audio' ? 'AUDIO' : 'VIDEO', document.title);
                                    }
                                    if (node.querySelectorAll) {
                                        var subMedia = node.querySelectorAll('video, audio, source');
                                        for (var k = 0; k < subMedia.length; k++) {
                                            var sub = subMedia[k];
                                            var subSrc = sub.currentSrc || sub.src || sub.getAttribute('src');
                                            if (subSrc) reportMedia(subSrc, sub.tagName.toLowerCase() === 'audio' ? 'AUDIO' : 'VIDEO', document.title);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });

                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                } else {
                    document.addEventListener('DOMContentLoaded', function() {
                        if (document.body) observer.observe(document.body, { childList: true, subtree: true });
                    });
                }
            } catch(e) {}

            scanDom();
            setTimeout(scanDom, 800);
            setTimeout(scanDom, 2500);
        })();
    """.trimIndent()
}
