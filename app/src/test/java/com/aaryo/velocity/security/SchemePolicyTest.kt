package com.aaryo.velocity.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchemePolicyTest {

    @Test
    fun testWebSchemes() {
        assertTrue(SchemePolicy.isWebScheme("http://example.com"))
        assertTrue(SchemePolicy.isWebScheme("https://example.com/path"))
        assertFalse(SchemePolicy.isWebScheme("ftp://example.com"))
        assertFalse(SchemePolicy.isWebScheme("file:///data/local/tmp"))
        assertFalse(SchemePolicy.isWebScheme("tel:1234567890"))
    }

    @Test
    fun testExternalSchemes() {
        assertTrue(SchemePolicy.isExternalScheme("tel:+1234567890"))
        assertTrue(SchemePolicy.isExternalScheme("mailto:user@example.com"))
        assertTrue(SchemePolicy.isExternalScheme("sms:123456?body=hello"))
        assertTrue(SchemePolicy.isExternalScheme("geo:37.7749,-122.4194"))
        assertTrue(SchemePolicy.isExternalScheme("market://details?id=com.example.app"))
        assertTrue(SchemePolicy.isExternalScheme("intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end"))
        assertFalse(SchemePolicy.isExternalScheme("https://example.com"))
    }

    @Test
    fun testBlockedSchemes() {
        assertTrue(SchemePolicy.isBlockedScheme("file:///android_asset/secret.txt"))
        assertTrue(SchemePolicy.isBlockedScheme("content://com.android.providers.media/external"))
        assertFalse(SchemePolicy.isBlockedScheme("https://example.com"))
        assertFalse(SchemePolicy.isBlockedScheme("about:blank"))
    }

    @Test
    fun testBrowserCustomSchemes() {
        assertTrue(SchemePolicy.isBrowserCustomScheme("googlechrome://navigate?url=https%3A%2F%2Fgoogle.com"))
        assertTrue(SchemePolicy.isBrowserCustomScheme("googlechromes://navigate?url=https%3A%2F%2Fgoogle.com"))
        assertTrue(SchemePolicy.isBrowserCustomScheme("chromenative://"))
        assertTrue(SchemePolicy.isBrowserCustomScheme("chrome://flags"))
        assertTrue(SchemePolicy.isBrowserCustomScheme("android-app://com.android.chrome"))
        assertFalse(SchemePolicy.isBrowserCustomScheme("https://google.com"))
        assertFalse(SchemePolicy.isBrowserCustomScheme("tel:123"))
    }
}
