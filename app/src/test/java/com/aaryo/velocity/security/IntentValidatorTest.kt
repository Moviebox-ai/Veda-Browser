package com.aaryo.velocity.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentValidatorTest {

    @Test
    fun testBrowserPackageDetection() {
        assertTrue(IntentValidator.isBrowserPackage("com.android.chrome"))
        assertTrue(IntentValidator.isBrowserPackage("com.chrome.beta"))
        assertTrue(IntentValidator.isBrowserPackage("com.google.android.googlequicksearchbox"))
        assertTrue(IntentValidator.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(IntentValidator.isBrowserPackage("com.brave.browser"))
        assertTrue(IntentValidator.isBrowserPackage("com.sec.android.app.sbrowser"))
        assertFalse(IntentValidator.isBrowserPackage("com.aaryo.velocity"))
        assertFalse(IntentValidator.isBrowserPackage("com.whatsapp"))
    }

    @Test
    fun testBrowserIntentDetection() {
        assertTrue(IntentValidator.isBrowserIntent("googlechrome://navigate?url=https%3A%2F%2Fwww.google.com"))
        assertTrue(IntentValidator.isBrowserIntent("googlechromes://navigate?url=https%3A%2F%2Fwww.google.com"))
        assertTrue(IntentValidator.isBrowserIntent("chromenative://open"))
        assertTrue(IntentValidator.isBrowserIntent("chrome://version"))
        assertTrue(IntentValidator.isBrowserIntent("android-app://com.android.chrome/https/www.google.com"))
        assertTrue(IntentValidator.isBrowserIntent("intent://www.google.com/search?q=test#Intent;package=com.android.chrome;end"))
        assertFalse(IntentValidator.isBrowserIntent("tel:+1234567890"))
        assertFalse(IntentValidator.isBrowserIntent("mailto:test@example.com"))
    }

    @Test
    fun testExtractWebUrlFromGoogleChromeSchemes() {
        // googlechrome://navigate?url=...
        val extracted1 = IntentValidator.extractWebUrlFromIntent("googlechrome://navigate?url=https%3A%2F%2Fwww.google.com%2Fsearch%3Fq%3Dandroid")
        assertEquals("https://www.google.com/search?q=android", extracted1)

        // googlechromes://navigate?url=...
        val extracted2 = IntentValidator.extractWebUrlFromIntent("googlechromes://navigate?url=https%3A%2F%2Fduckduckgo.com%2F%3Fq%3Dtest")
        assertEquals("https://duckduckgo.com/?q=test", extracted2)

        // googlechrome://www.google.com/search?q=velocity
        val extracted3 = IntentValidator.extractWebUrlFromIntent("googlechrome://www.google.com/search?q=velocity")
        assertEquals("http://www.google.com/search?q=velocity", extracted3)

        // googlechromes://www.google.com/search?q=velocity
        val extracted4 = IntentValidator.extractWebUrlFromIntent("googlechromes://www.google.com/search?q=velocity")
        assertEquals("https://www.google.com/search?q=velocity", extracted4)
    }

    @Test
    fun testExtractWebUrlFromAndroidAppLink() {
        val extracted = IntentValidator.extractWebUrlFromIntent("android-app://com.android.chrome/https/www.google.com/search?q=android")
        assertEquals("https://www.google.com/search?q=android", extracted)
    }

    @Test
    fun testExtractWebUrlFromIntentUri() {
        // With scheme=https in fragment
        val intentUri1 = "intent://www.google.com/search?q=kotlin#Intent;scheme=https;package=com.android.chrome;end"
        val extracted1 = IntentValidator.extractWebUrlFromIntent(intentUri1)
        assertEquals("https://www.google.com/search?q=kotlin", extracted1)

        // With browser_fallback_url in fragment
        val intentUri2 = "intent:#Intent;action=android.intent.action.VIEW;category=android.intent.category.BROWSABLE;package=com.android.chrome;S.browser_fallback_url=https%3A%2F%2Fwww.google.com%2Fsearch%3Fq%3Dcompose;end"
        val extracted2 = IntentValidator.extractWebUrlFromIntent(intentUri2)
        assertEquals("https://www.google.com/search?q=compose", extracted2)

        // Direct intent:https
        val intentUri3 = "intent:https://news.google.com/#Intent;end"
        val extracted3 = IntentValidator.extractWebUrlFromIntent(intentUri3)
        assertEquals("https://news.google.com/", extracted3)
    }

    @Test
    fun testExtractWebUrlReturnsNullForNonWeb() {
        assertNull(IntentValidator.extractWebUrlFromIntent("tel:1234567890"))
        assertNull(IntentValidator.extractWebUrlFromIntent("mailto:user@example.com"))
        assertNull(IntentValidator.extractWebUrlFromIntent("geo:0,0?q=tokyo"))
    }
}
