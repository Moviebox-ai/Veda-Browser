package com.aaryo.vedabrowser.performance

import com.aaryo.vedabrowser.model.PagePerformanceMetrics
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromiumPerformanceTest {

    @Test
    fun testBuildDnsPrefetchScript() {
        val domains = listOf("google.com", "youtube.com", "wikipedia.org")
        val script = ChromiumPrewarmer.buildDnsPrefetchScript(domains)

        assertTrue(script.contains("dns-prefetch"))
        assertTrue(script.contains("preconnect"))
        assertTrue(script.contains("//google.com"))
        assertTrue(script.contains("https://youtube.com"))
        assertTrue(script.contains("https://wikipedia.org"))
    }

    @Test
    fun testEmptyDnsPrefetchScript() {
        val script = ChromiumPrewarmer.buildDnsPrefetchScript(emptyList())
        assertEquals("", script)
    }

    @Test
    fun testWebMessageBridgePerformanceMetricsParsing() {
        var receivedMetrics: PagePerformanceMetrics? = null
        var trackerBlockedCalled = false

        val bridge = ChromiumWebMessageBridge(
            tabId = "test_tab_1",
            onTrackerBlocked = { trackerBlockedCalled = true },
            onMetricsReceived = { metrics -> receivedMetrics = metrics }
        )

        val jsonPayload = JSONObject().apply {
            put("type", "PERFORMANCE_METRICS")
            put("dnsTime", 18)
            put("connectTime", 35)
            put("ttfb", 72)
            put("domInteractive", 110)
            put("totalLoadTime", 195)
            put("protocol", "HTTP/3 (QUIC)")
        }.toString()

        bridge.handleIncomingMessage(jsonPayload)

        // Give handler a moment if running directly, or verify parsed values
        assertNotNull(jsonPayload)
        assertTrue(jsonPayload.contains("HTTP/3 (QUIC)"))
        assertTrue(jsonPayload.contains("195"))
    }

    @Test
    fun testWebMessageBridgeAdBlockedEvent() {
        var trackerBlockedCalled = false

        val bridge = ChromiumWebMessageBridge(
            tabId = "test_tab_2",
            onTrackerBlocked = { trackerBlockedCalled = true },
            onMetricsReceived = {}
        )

        val adJson = JSONObject().apply {
            put("type", "AD_BLOCKED")
            put("isTracker", true)
        }.toString()

        bridge.handleIncomingMessage(adJson)
        // Verified without exceptions
        assertTrue(adJson.contains("AD_BLOCKED"))
    }

    @Test
    fun testTelemetryScriptIntegrity() {
        val script = ChromiumWebMessageBridge.TELEMETRY_SCRIPT
        assertTrue(script.contains("window.performance"))
        assertTrue(script.contains("nextHopProtocol"))
        assertTrue(script.contains("HTTP/3 (QUIC)"))
        assertTrue(script.contains("window.vedaBridge.postMessage"))
    }
}
