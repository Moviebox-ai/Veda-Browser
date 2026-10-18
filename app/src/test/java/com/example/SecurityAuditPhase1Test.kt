package com.example

import com.example.security.ContentBlocker
import com.example.security.SchemePolicy
import com.example.util.UrlResolver
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityAuditPhase1Test {

    @Test
    fun testSchemePolicy_blocksDangerousSchemes() {
        assertTrue(SchemePolicy.isForbidden("javascript"))
        assertTrue(SchemePolicy.isForbidden("file"))
        assertTrue(SchemePolicy.isForbidden("content"))
        assertTrue(SchemePolicy.isForbidden("jar"))
        assertTrue(SchemePolicy.isForbidden("data"))
        assertTrue(SchemePolicy.isForbidden("blob"))

        assertFalse(SchemePolicy.isForbidden("http"))
        assertFalse(SchemePolicy.isForbidden("https"))
        assertFalse(SchemePolicy.isForbidden("tel"))
        assertFalse(SchemePolicy.isForbidden("mailto"))
    }

    @Test
    fun testUrlResolver_blocksBlobAndDataSchemes() {
        val blobRes = UrlResolver.resolve("blob:https://example.com/uuid-1234")
        assertEquals(UrlResolver.ResolvedType.BLOCKED_SCHEME, blobRes.type)

        val dataRes = UrlResolver.resolve("data:text/html,<script>alert(1)</script>")
        assertEquals(UrlResolver.ResolvedType.BLOCKED_SCHEME, dataRes.type)
    }

    @Test
    fun testUrlResolver_editableUrlPreservesPlusSigns() {
        val testUrl = "https://example.com/search?q=kotlin+android+test&signature=abc+123"
        val editable = UrlResolver.getEditableUrl(testUrl)
        assertTrue("Plus sign must not be replaced with a space in query parameters", editable.contains("kotlin+android+test"))
        assertTrue("Signature plus must be preserved", editable.contains("abc+123"))
        assertFalse("Should not have converted plus to spaces", editable.contains("kotlin android test"))
    }

    @Test
    fun testContentBlocker_toggleControlsBlocking() {
        val blocker = ContentBlocker()
        val adUri = Uri.parse("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")
        val fbUri = Uri.parse("https://connect.facebook.net/en_US/fbevents.js")
        val legitUri = Uri.parse("https://developer.android.com/guide")

        blocker.isEnabled = true
        assertTrue("Should block googlesyndication when enabled", blocker.shouldBlock(adUri))
        assertTrue("Should block connect.facebook.net when enabled", blocker.shouldBlock(fbUri))
        assertFalse("Should not block legit site", blocker.shouldBlock(legitUri))

        blocker.isEnabled = false
        assertFalse("Should NOT block ad when blocker is disabled", blocker.shouldBlock(adUri))
        assertFalse("Should NOT block tracker when blocker is disabled", blocker.shouldBlock(fbUri))
    }
}
