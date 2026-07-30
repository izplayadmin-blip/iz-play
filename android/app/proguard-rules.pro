# Media3 / ExoPlayer
-dontwarn com.google.android.exoplayer2.**

# kotlinx.serialization — mantém os serializers gerados dos modelos
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.izplay.tv.**$$serializer { *; }
-keepclassmembers class com.izplay.tv.** {
    *** Companion;
}
-keepclasseswithmembers class com.izplay.tv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# SwarmCloud P2P SDK
-dontwarn com.p2pengine.**
-keep class com.p2pengine.** { *; }
-keep interface com.p2pengine.** { *; }
-keep class com.cdnbye.libdc.** { *; }
-keep interface com.cdnbye.libdc.** { *; }
-keep class com.snapchat.djinni.** { *; }
