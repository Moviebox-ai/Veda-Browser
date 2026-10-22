package com.aaryo.vedabrowser.extension

import com.aaryo.vedabrowser.model.Extension
import com.aaryo.vedabrowser.model.RunAt

object BuiltInExtensions {

    fun getAll(): List<Extension> {
        return listOf(
            createDarkModeExtension(),
            createCosmeticAdHideExtension(),
            createCookieBannerDismissExtension(),
            createVideoBackgroundPlayExtension(),
            createForceReaderExtension(),
            createCustomCssExtension()
        )
    }

    private fun createDarkModeExtension(): Extension {
        val css = """
            html {
                filter: invert(1) hue-rotate(180deg) !important;
                background-color: #121212 !important;
            }
            img, video, canvas, svg, picture, embed, object {
                filter: invert(1) hue-rotate(180deg) !important;
            }
        """.trimIndent().replace("\n", " ")

        val js = """
            // ==UserScript==
            // @name         Universal Dark Mode
            // @version      1.2
            // @description  Applies dark mode color inversion to any webpage while preserving media colors
            // @author       Velocity Built-in
            // @match        <all_urls>
            // @run-at       document-start
            // @grant        GM_addStyle
            // ==/UserScript==
            (function() {
                var style = document.createElement('style');
                style.id = 'velocity-dark-mode-style';
                style.textContent = '$css';
                (document.head || document.documentElement).appendChild(style);
            })();
        """.trimIndent()

        return UserscriptParser.parse(js).copy(
            id = "builtin_dark_mode",
            isBuiltIn = true,
            enabled = false,
            allowInIncognito = true
        )
    }

    private fun createCosmeticAdHideExtension(): Extension {
        val selectors = listOf(
            ".ad-banner", ".adsbygoogle", "[id^='google_ads_']", ".ad-container",
            ".sponsored-post", ".taboola-placeholder", ".outbrain-placeholder",
            "[data-ad-slot]", ".ad-placement", ".advertisement", ".ad-wrapper"
        ).joinToString(", ")

        val js = """
            // ==UserScript==
            // @name         Cosmetic Ad-Hide
            // @version      1.1
            // @description  Hides visible banner slots, sponsored sections, and empty advertisement containers
            // @author       Velocity Built-in
            // @match        <all_urls>
            // @run-at       document-start
            // @grant        GM_addStyle
            // ==/UserScript==
            (function() {
                var style = document.createElement('style');
                style.id = 'velocity-cosmetic-adhide';
                style.textContent = '$selectors { display: none !important; visibility: hidden !important; height: 0 !important; margin: 0 !important; padding: 0 !important; }';
                (document.head || document.documentElement).appendChild(style);
            })();
        """.trimIndent()

        return UserscriptParser.parse(js).copy(
            id = "builtin_cosmetic_adhide",
            isBuiltIn = true,
            enabled = true,
            allowInIncognito = true
        )
    }

    private fun createCookieBannerDismissExtension(): Extension {
        val js = """
            // ==UserScript==
            // @name         Auto-Accept Cookie Banners
            // @version      1.0
            // @description  Automatically clicks consent and accept buttons on common cookie CMP prompts
            // @author       Velocity Built-in
            // @match        <all_urls>
            // @run-at       document-idle
            // @grant        none
            // ==/UserScript==
            (function() {
                var selectors = [
                    '#onetrust-accept-btn-handler',
                    '.cc-btn.cc-allow',
                    '#accept-all-cookies',
                    '#CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll',
                    'button[data-testid="accept-cookies"]',
                    'button#didomi-notice-agree-button',
                    'a.cmpboxbtnno'
                ];
                for (var i = 0; i < selectors.length; i++) {
                    var btn = document.querySelector(selectors[i]);
                    if (btn && typeof btn.click === 'function') {
                        btn.click();
                        break;
                    }
                }
            })();
        """.trimIndent()

        return UserscriptParser.parse(js).copy(
            id = "builtin_cookie_accept",
            isBuiltIn = true,
            enabled = false,
            allowInIncognito = true
        )
    }

    private fun createVideoBackgroundPlayExtension(): Extension {
        val js = """
            // ==UserScript==
            // @name         Video Background Play
            // @version      1.3
            // @description  Prevents video players from pausing automatically when switching tabs or backgrounding
            // @author       Velocity Built-in
            // @match        *://*.youtube.com/*
            // @match        *://youtube.com/*
            // @match        *://*.vimeo.com/*
            // @run-at       document-start
            // @grant        none
            // ==/UserScript==
            (function() {
                try {
                    Object.defineProperty(document, 'hidden', { value: false, writable: false });
                    Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: false });
                    window.addEventListener('visibilitychange', function(e) {
                        e.stopImmediatePropagation();
                    }, true);
                } catch(e) {}
            })();
        """.trimIndent()

        return UserscriptParser.parse(js).copy(
            id = "builtin_video_bg_play",
            isBuiltIn = true,
            enabled = true,
            allowInIncognito = true
        )
    }

    private fun createForceReaderExtension(): Extension {
        val js = """
            // ==UserScript==
            // @name         Force Reader Typography
            // @version      1.0
            // @description  Improves readability of article pages with clean serif typography and centered margins
            // @author       Velocity Built-in
            // @match        <all_urls>
            // @run-at       document-end
            // @grant        none
            // ==/UserScript==
            (function() {
                var article = document.querySelector('article') || document.querySelector('.post-content') || document.querySelector('main');
                if (article) {
                    article.style.maxWidth = '720px';
                    article.style.margin = '0 auto';
                    article.style.fontSize = '18px';
                    article.style.lineHeight = '1.8';
                    article.style.fontFamily = 'Georgia, serif';
                }
            })();
        """.trimIndent()

        return UserscriptParser.parse(js).copy(
            id = "builtin_force_reader",
            isBuiltIn = true,
            enabled = false,
            allowInIncognito = false
        )
    }

    private fun createCustomCssExtension(): Extension {
        val js = """
            // ==UserScript==
            // @name         Custom Site Styler
            // @version      1.0
            // @description  Injects clean site enhancements and typography rules
            // @author       Velocity Built-in
            // @match        <all_urls>
            // @run-at       document-end
            // @grant        GM_addStyle
            // ==/UserScript==
            (function() {
                // Example: Enhances font rendering smoothness
                var style = document.createElement('style');
                style.textContent = 'body { -webkit-font-smoothing: antialiased; }';
                document.head.appendChild(style);
            })();
        """.trimIndent()

        return UserscriptParser.parse(js).copy(
            id = "builtin_custom_styler",
            isBuiltIn = true,
            enabled = false,
            allowInIncognito = false
        )
    }
}
