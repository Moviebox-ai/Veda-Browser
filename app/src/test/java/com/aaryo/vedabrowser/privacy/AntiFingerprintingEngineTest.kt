package com.aaryo.vedabrowser.privacy

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiFingerprintingEngineTest {

    @Test
    fun testAntiFingerprintScriptContainsCanvasProtection() {
        val script = AntiFingerprintingEngine.getAntiFingerprintScript()
        assertNotNull(script)
        assertTrue(script.contains("CanvasRenderingContext2D.prototype.getImageData"))
        assertTrue(script.contains("HTMLCanvasElement.prototype.toDataURL"))
        assertTrue(script.contains("HTMLCanvasElement.prototype.toBlob"))
    }

    @Test
    fun testAntiFingerprintScriptContainsWebGLSpoofing() {
        val script = AntiFingerprintingEngine.getAntiFingerprintScript()
        assertTrue(script.contains("Google Inc. (Google)"))
        assertTrue(script.contains("ANGLE (Google, Vulkan 1.3.0, Android Generic)"))
        assertTrue(script.contains("WebGLRenderingContext"))
        assertTrue(script.contains("37445")) // UNMASKED_VENDOR_WEBGL
        assertTrue(script.contains("37446")) // UNMASKED_RENDERER_WEBGL
    }

    @Test
    fun testAntiFingerprintScriptContainsAudioAndHardwareNormalization() {
        val script = AntiFingerprintingEngine.getAntiFingerprintScript()
        assertTrue(script.contains("OfflineAudioContext"))
        assertTrue(script.contains("AnalyserNode"))
        assertTrue(script.contains("hardwareConcurrency"))
        assertTrue(script.contains("deviceMemory"))
        assertTrue(script.contains("maxTouchPoints"))
        assertTrue(script.contains("colorDepth"))
    }

    @Test
    fun testAntiFingerprintScriptContainsClientHintsAndBatterySpoofing() {
        val script = AntiFingerprintingEngine.getAntiFingerprintScript()
        assertTrue(script.contains("userAgentData"))
        assertTrue(script.contains("getHighEntropyValues"))
        assertTrue(script.contains("getBattery"))
        assertTrue(script.contains("charging: true"))
    }
}
