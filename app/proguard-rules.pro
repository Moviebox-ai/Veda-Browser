# ProGuard / R8 Rules for Velocity Browser

# Keep Gson Models and serialized fields
-keepattributes *Annotation*
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.aaryo.vedabrowser.model.** { *; }
-keepclassmembers class com.aaryo.vedabrowser.model.** { *; }

# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebChromeClient and WebViewClient subclasses and their public methods
-keep public class * extends android.webkit.WebChromeClient {
    public *;
}
-keep public class * extends android.webkit.WebViewClient {
    public *;
}

# Keep Jetpack Compose runtime & material3
-keep class androidx.compose.material3.** { *; }
