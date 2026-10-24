package com.aaryo.vedabrowser.performance

/**
 * Chromium Background Media Playback Engine.
 *
 * Prevents YouTube, Spotify Web, podcasts, and HTML5 video/audio players from pausing
 * when switching tabs, minimizing the browser, locking the screen, or opening menus.
 *
 * Hooks into Blink's Page Visibility API (document.hidden, document.visibilityState)
 * and intercepts blur/visibilitychange event handlers.
 */
object BackgroundMediaEngine {

    const val BACKGROUND_MEDIA_SCRIPT = """
        (function() {
            if (window.__vedaBackgroundMediaInstalled) return;
            window.__vedaBackgroundMediaInstalled = true;

            try {
                // 1. Force Page Visibility API to always report 'visible' and not hidden
                Object.defineProperty(document, 'hidden', {
                    get: function() { return false; },
                    configurable: true
                });
                Object.defineProperty(document, 'visibilityState', {
                    get: function() { return 'visible'; },
                    configurable: true
                });
                Object.defineProperty(document, 'webkitVisibilityState', {
                    get: function() { return 'visible'; },
                    configurable: true
                });
            } catch(e) {}

            // 2. Filter out visibilitychange and blur events that pages use to pause playback
            try {
                var originalAddEventListener = EventTarget.prototype.addEventListener;
                EventTarget.prototype.addEventListener = function(type, listener, options) {
                    if (type === 'visibilitychange' || type === 'webkitvisibilitychange') {
                        // Prevent the listener from ever seeing a 'hidden' state
                        var safeListener = function(event) {
                            try {
                                if (document.visibilityState === 'visible') {
                                    listener.call(this, event);
                                }
                            } catch(err) {}
                        };
                        return originalAddEventListener.call(this, type, safeListener, options);
                    }
                    return originalAddEventListener.call(this, type, listener, options);
                };

                // Also prevent direct document.onvisibilitychange assignment from receiving hidden states
                var originalOnVisibilityChange = null;
                Object.defineProperty(document, 'onvisibilitychange', {
                    get: function() { return originalOnVisibilityChange; },
                    set: function(handler) {
                        originalOnVisibilityChange = function(e) {
                            if (document.visibilityState === 'visible' && typeof handler === 'function') {
                                handler.call(this, e);
                            }
                        };
                    },
                    configurable: true
                });
            } catch(e) {}
        })();
    """
}
