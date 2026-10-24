package com.aaryo.vedabrowser.download

import com.aaryo.vedabrowser.model.SniffedMediaItem
import com.aaryo.vedabrowser.model.SniffedMediaType
import com.aaryo.vedabrowser.performance.ChromiumWebMessageBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaSnifferTest {

    @Before
    fun setUp() {
        MediaSnifferEngine.clearTabMedia("test_tab_1")
        MediaSnifferEngine.clearTabMedia("test_tab_2")
    }

    @Test
    fun testIsMediaUrlDirectVideos() {
        assertTrue(MediaSnifferEngine.isMediaUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/stream/video.webm"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/movie.mkv"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/video.ts"))
    }

    @Test
    fun testIsMediaUrlStreamsAndAudio() {
        assertTrue(MediaSnifferEngine.isMediaUrl("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/manifest.mpd"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/music/song.mp3"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/audio/voice.m4a"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/sound.wav"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://example.com/track.flac"))
    }

    @Test
    fun testIsMediaUrlQueryParamsAndPatterns() {
        assertTrue(MediaSnifferEngine.isMediaUrl("https://video.xx.fbcdn.net/v/t42.1797-2/1000.mp4?_nc_cat=1&token=abc"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://rr1---sn-4g5edn6s.googlevideo.com/videoplayback?expire=123&mime=video%2Fmp4"))
        assertTrue(MediaSnifferEngine.isMediaUrl("https://cdn.example.com/live/hls/master.m3u8?auth=token"))
    }

    @Test
    fun testIsMediaUrlRejectsNonMedia() {
        assertFalse(MediaSnifferEngine.isMediaUrl("https://example.com/index.html"))
        assertFalse(MediaSnifferEngine.isMediaUrl("https://example.com/scripts/bundle.js"))
        assertFalse(MediaSnifferEngine.isMediaUrl("https://example.com/css/main.css"))
        assertFalse(MediaSnifferEngine.isMediaUrl("https://example.com/images/avatar.jpg"))
        assertFalse(MediaSnifferEngine.isMediaUrl("https://example.com/images/logo.png"))
        assertFalse(MediaSnifferEngine.isMediaUrl("https://example.com/fonts/roboto.woff2"))
        assertFalse(MediaSnifferEngine.isMediaUrl("javascript:void(0)"))
        assertFalse(MediaSnifferEngine.isMediaUrl("data:image/svg+xml;base64,PHN2Zz4="))
        assertFalse(MediaSnifferEngine.isMediaUrl(null))
        assertFalse(MediaSnifferEngine.isMediaUrl(""))
    }

    @Test
    fun testFormattedSizeCalculations() {
        val smallVideo = SniffedMediaItem(
            tabId = "t1",
            url = "https://example.com/video.mp4",
            filename = "video.mp4",
            sizeBytes = 512 * 1024L
        )
        assertEquals("512.0 KB", smallVideo.formattedSize)

        val mediumVideo = SniffedMediaItem(
            tabId = "t1",
            url = "https://example.com/movie.mp4",
            filename = "movie.mp4",
            sizeBytes = 25 * 1024 * 1024L
        )
        assertEquals("25.0 MB", mediumVideo.formattedSize)

        val largeVideo = SniffedMediaItem(
            tabId = "t1",
            url = "https://example.com/4k.mp4",
            filename = "4k.mp4",
            sizeBytes = 2500 * 1024 * 1024L
        )
        assertEquals("2.44 GB", largeVideo.formattedSize)

        val stream = SniffedMediaItem(
            tabId = "t1",
            url = "https://example.com/stream.m3u8",
            filename = "stream.m3u8",
            mediaType = SniffedMediaType.STREAM,
            sizeBytes = -1L
        )
        assertEquals("Live / HLS Stream", stream.formattedSize)
    }

    @Test
    fun testMediaSnifferDeduplicationAndClearing() {
        val tabId = "test_tab_1"
        assertEquals(0, MediaSnifferEngine.getMediaCount(tabId))

        MediaSnifferEngine.onMediaDetected(
            tabId = tabId,
            url = "https://example.com/bunny_1080p.mp4",
            pageUrl = "https://example.com",
            pageTitle = "Big Buck Bunny"
        )

        assertEquals(1, MediaSnifferEngine.getMediaCount(tabId))

        // Adding exact same URL again should be deduplicated
        MediaSnifferEngine.onMediaDetected(
            tabId = tabId,
            url = "https://example.com/bunny_1080p.mp4",
            pageUrl = "https://example.com",
            pageTitle = "Big Buck Bunny"
        )
        assertEquals(1, MediaSnifferEngine.getMediaCount(tabId))

        // Adding different URL
        MediaSnifferEngine.onMediaDetected(
            tabId = tabId,
            url = "https://example.com/audio_theme.mp3",
            pageUrl = "https://example.com",
            pageTitle = "Theme Song"
        )
        assertEquals(2, MediaSnifferEngine.getMediaCount(tabId))

        val items = MediaSnifferEngine.getMediaForTab(tabId)
        assertEquals(2, items.size)
        assertTrue(items.any { it.mediaType == SniffedMediaType.VIDEO })
        assertTrue(items.any { it.mediaType == SniffedMediaType.AUDIO })

        // Clear media for this tab
        MediaSnifferEngine.clearTabMedia(tabId)
        assertEquals(0, MediaSnifferEngine.getMediaCount(tabId))
        assertTrue(MediaSnifferEngine.getMediaForTab(tabId).isEmpty())
    }

    @Test
    fun testWebMessageBridgeMediaDetectedMessage() {
        var detectedUrl: String? = null
        var detectedTitle: String? = null
        var detectedType: String? = null

        val bridge = ChromiumWebMessageBridge(
            tabId = "test_tab_1",
            onTrackerBlocked = {},
            onMetricsReceived = {},
            onMediaDetected = { url, _, title, _, type ->
                detectedUrl = url
                detectedTitle = title
                detectedType = type
            }
        )

        val payload = """
            {
                "type": "MEDIA_DETECTED",
                "url": "https://cdn.example.com/trailer_1080p.mp4",
                "pageUrl": "https://example.com/watch",
                "pageTitle": "Sample Trailer Movie",
                "mimeType": "video/mp4",
                "mediaType": "VIDEO"
            }
        """.trimIndent()

        bridge.handleIncomingMessage(payload)

        assertEquals("https://cdn.example.com/trailer_1080p.mp4", detectedUrl)
        assertEquals("Sample Trailer Movie", detectedTitle)
        assertEquals("VIDEO", detectedType)
    }

    @Test
    fun testMediaSnifferScriptInvariants() {
        val script = MediaSnifferEngine.MEDIA_SNIFFER_SCRIPT
        assertNotNull(script)
        assertTrue(script.contains("__vedaMediaSnifferInstalled"))
        assertTrue(script.contains("vedaScanMediaNow"))
        assertTrue(script.contains("MEDIA_DETECTED"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("HTMLMediaElement.prototype.play"))
        assertTrue(script.contains("querySelectorAll('video, audio, source"))
    }
}
