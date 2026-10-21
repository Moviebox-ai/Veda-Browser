package com.aaryo.velocity.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentValidatorTest {

    @Test
    fun testIsBrowserPackage() {
        assertTrue(IntentValidator.isBrowserPackage("com.android.chrome"))
        assertTrue(IntentValidator.isBrowserPackage("com.chrome.beta"))
        assertTrue(IntentValidator.isBrowserPackage("com.google.android.googlequicksearchbox"))
        assertTrue(IntentValidator.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(IntentValidator.isBrowserPackage("com.opera.browser"))
        assertTrue(IntentValidator.isBrowserPackage("com.brave.browser"))
        assertFalse(IntentValidator.isBrowserPackage("com.aaryo.velocity"))
        assertFalse(IntentValidator.isBrowserPackage("com.whatsapp"))
        assertFalse(IntentValidator.isBrowserPackage("com.google.android.apps.maps"))
    }

    @Test
    fun testIsBrowserIntent() {
        assertTrue(IntentValidator.isBrowserIntent("googlechrome://navigate?url=https%3A%2F%2Fgoogle.com"))
        assertTrue(IntentValidator.isBrowserIntent("googlechromes://navigate?url=https%3A%2F%2Fgoogle.com"))
        assertTrue(IntentValidator.isBrowserIntent("chromenative://"))
        assertTrue(IntentValidator.isBrowserIntent("chrome://version"))
        assertTrue(IntentValidator.isBrowserIntent("android-app://com.android.chrome"))
        assertTrue(IntentValidator.isBrowserIntent("intent://www.google.com/#Intent;package=com.android.chrome;end"))
        assertTrue(IntentValidator.isBrowserIntent("intent://www.google.com/#Intent;category=android.intent.category.browsable;end"))
        assertFalse(IntentValidator.isBrowserIntent("https://www.google.com"))
        assertFalse(IntentValidator.isBrowserIntent("tel:123456"))
    }

    @Test
    fun testExtractWebUrlFromChromeNavigate() {
        val googleNav = "googlechrome://navigate?url=https%3A%2F%2Fwww.google.com%2Fsearch%3Fq%3Dandroid"
        val extracted = IntentValidator.extractWebUrlFromIntent(googleNav)
        assertNotNull(extracted)
        assertEquals("https://www.google.com/search?q=android", extracted)

        val chromesNav = "googlechromes://navigate?url=https%3A%2F%2Fwww.google.com%2Fsearch%3Fq%3Dcricket"
        val extracted2 = IntentValidator.extractWebUrlFromIntent(chromesNav)
        assertNotNull(extracted2)
        assertEquals("https://www.google.com/search?q=cricket", extracted2)
    }

    @Test
    fun testExtractWebUrlFromAndroidAppLinks() {
        val appLink = "android-app://com.android.chrome/https/www.google.com/search?q=velocity"
        val extracted = IntentValidator.extractWebUrlFromIntent(appLink)
        assertNotNull(extracted)
        assertEquals("https://www.google.com/search?q=velocity", extracted)
    }

    @Test
    fun testExtractWebUrlFromIntentScheme() {
        val intentUri = "intent://www.google.com/search?q=kotlin#Intent;scheme=https;package=com.android.chrome;end"
        val extracted = IntentValidator.extractWebUrlFromIntent(intentUri)
        assertNotNull(extracted)
        assertEquals("https://www.google.com/search?q=kotlin", extracted)

        val fallbackUri = "intent://open?id=123#Intent;package=com.some.app;S.browser_fallback_url=https%3A%2F%2Fexample.com%2Ffallback;end"
        val extractedFallback = IntentValidator.extractWebUrlFromIntent(fallbackUri)
        assertNotNull(extractedFallback)
        assertEquals("https://example.com/fallback", extractedFallback)
    }

    @Test
    fun testEmptyOrInvalidIntentReturnsNull() {
        assertNull(IntentValidator.extractWebUrlFromIntent(""))
        assertNull(IntentValidator.extractWebUrlFromIntent("   "))
        assertNull(IntentValidator.extractWebUrlFromIntent("tel:123456"))
        assertNull(IntentValidator.extractWebUrlFromIntent("market://details?id=com.app"))
    }
}
