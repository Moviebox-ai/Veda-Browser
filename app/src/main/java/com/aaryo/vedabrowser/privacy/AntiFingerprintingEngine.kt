package com.aaryo.vedabrowser.privacy

/**
 * Chromium Anti-Fingerprinting & Hardware Cloaking Engine.
 *
 * Defeats modern fingerprinting techniques used by trackers (FingerprintJS, CreepJS, etc.)
 * by cloaking Canvas rasterization, WebGL GPU parameters, AudioContext frequencies,
 * Client Hints, Battery status, and device hardware metrics without breaking legitimate websites.
 */
object AntiFingerprintingEngine {

    /**
     * JavaScript payload injected at document start into Chromium V8 runtime.
     */
    fun getAntiFingerprintScript(): String {
        return """
            (function() {
                if (window.__vedaAntiFingerprintActive) return;
                window.__vedaAntiFingerprintActive = true;

                // 1. Canvas Fingerprinting Protection (Subtle Noise Cloaking)
                try {
                    var originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                    CanvasRenderingContext2D.prototype.getImageData = function(x, y, w, h) {
                        var imageData = originalGetImageData.apply(this, arguments);
                        try {
                            var data = imageData.data;
                            var len = data.length;
                            // Add deterministic pseudo-noise on a few pixels to disrupt hash matching
                            for (var i = 0; i < len; i += 64) {
                                var noise = ((i % 3 === 0) ? 1 : -1);
                                data[i] = Math.min(255, Math.max(0, data[i] + noise));
                            }
                        } catch(e) {}
                        return imageData;
                    };

                    var originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                    HTMLCanvasElement.prototype.toDataURL = function() {
                        try {
                            var ctx = this.getContext('2d');
                            if (ctx && this.width > 0 && this.height > 0) {
                                var imgData = originalGetImageData.call(ctx, 0, 0, Math.min(this.width, 16), Math.min(this.height, 16));
                                var d = imgData.data;
                                if (d.length > 4) {
                                    d[0] = Math.min(255, Math.max(0, d[0] + 1));
                                    ctx.putImageData(imgData, 0, 0);
                                }
                            }
                        } catch(e) {}
                        return originalToDataURL.apply(this, arguments);
                    };

                    if (HTMLCanvasElement.prototype.toBlob) {
                        var originalToBlob = HTMLCanvasElement.prototype.toBlob;
                        HTMLCanvasElement.prototype.toBlob = function(callback, type, quality) {
                            try {
                                var ctx = this.getContext('2d');
                                if (ctx && this.width > 0 && this.height > 0) {
                                    var imgData = originalGetImageData.call(ctx, 0, 0, Math.min(this.width, 16), Math.min(this.height, 16));
                                    var d = imgData.data;
                                    if (d.length > 4) {
                                        d[0] = Math.min(255, Math.max(0, d[0] + 1));
                                        ctx.putImageData(imgData, 0, 0);
                                    }
                                }
                            } catch(e) {}
                            return originalToBlob.apply(this, arguments);
                        };
                    }
                } catch(e) {}

                // 2. WebGL Hardware & GPU Vendor Spoofing
                try {
                    function wrapWebGL(proto) {
                        if (!proto) return;
                        var origGetParam = proto.getParameter;
                        proto.getParameter = function(param) {
                            // UNMASKED_VENDOR_WEBGL (0x9245)
                            if (param === 37445) {
                                return "Google Inc. (Google)";
                            }
                            // UNMASKED_RENDERER_WEBGL (0x9246)
                            if (param === 37446) {
                                return "ANGLE (Google, Vulkan 1.3.0, Android Generic)";
                            }
                            // MAX_VARYING_VECTORS / MAX_VERTEX_ATTRIBS normalization
                            if (param === 36348) return 30;
                            return origGetParam.apply(this, arguments);
                        };
                    }
                    if (window.WebGLRenderingContext) wrapWebGL(WebGLRenderingContext.prototype);
                    if (window.WebGL2RenderingContext) wrapWebGL(WebGL2RenderingContext.prototype);
                } catch(e) {}

                // 3. AudioContext Fingerprint Cloaking
                try {
                    if (window.OfflineAudioContext) {
                        var origStartRendering = OfflineAudioContext.prototype.startRendering;
                        OfflineAudioContext.prototype.startRendering = function() {
                            return origStartRendering.apply(this, arguments).then(function(buffer) {
                                try {
                                    for (var ch = 0; ch < buffer.numberOfChannels; ch++) {
                                        var data = buffer.getChannelData(ch);
                                        for (var i = 0; i < data.length; i += 100) {
                                            data[i] += 0.0000001 * (Math.random() - 0.5);
                                        }
                                    }
                                } catch(e) {}
                                return buffer;
                            });
                        };
                    }
                    if (window.AnalyserNode) {
                        var origGetFloatFreq = AnalyserNode.prototype.getFloatFrequencyData;
                        AnalyserNode.prototype.getFloatFrequencyData = function(array) {
                            origGetFloatFreq.apply(this, arguments);
                            for (var i = 0; i < array.length; i += 10) {
                                array[i] += 0.0001 * (Math.random() - 0.5);
                            }
                        };
                    }
                } catch(e) {}

                // 4. Device Metrics & Hardware Normalization
                try {
                    Object.defineProperty(navigator, 'hardwareConcurrency', {
                        get: function() { return 8; },
                        configurable: true
                    });
                    Object.defineProperty(navigator, 'deviceMemory', {
                        get: function() { return 8; },
                        configurable: true
                    });
                    Object.defineProperty(navigator, 'maxTouchPoints', {
                        get: function() { return 5; },
                        configurable: true
                    });
                    Object.defineProperty(screen, 'colorDepth', {
                        get: function() { return 24; },
                        configurable: true
                    });
                    Object.defineProperty(screen, 'pixelDepth', {
                        get: function() { return 24; },
                        configurable: true
                    });
                } catch(e) {}

                // 5. Battery API Tracking Neutralization
                try {
                    if (navigator.getBattery) {
                        navigator.getBattery = function() {
                            return Promise.resolve({
                                charging: true,
                                chargingTime: 0,
                                dischargingTime: Infinity,
                                level: 1.0,
                                addEventListener: function() {},
                                removeEventListener: function() {},
                                dispatchEvent: function() { return false; }
                            });
                        };
                    }
                } catch(e) {}

                // 6. Chromium User-Agent Client Hints High-Entropy Cloaking
                try {
                    if (navigator.userAgentData && navigator.userAgentData.getHighEntropyValues) {
                        var origGetHighEntropy = navigator.userAgentData.getHighEntropyValues;
                        navigator.userAgentData.getHighEntropyValues = function(hints) {
                            return origGetHighEntropy.apply(this, arguments).then(function(res) {
                                var clean = Object.assign({}, res);
                                if (clean.architecture) clean.architecture = "arm64";
                                if (clean.bitness) clean.bitness = "64";
                                if (clean.model) clean.model = "Generic Mobile";
                                if (clean.platformVersion) clean.platformVersion = "14.0.0";
                                return clean;
                            });
                        };
                    }
                } catch(e) {}
            })();
        """.trimIndent()
    }
}
