-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

-keep class com.my.vpn.data.model.** { *; }

-keep class libv2ray.** { *; }
-keep class go.** { *; }
-dontwarn libv2ray.**
-dontwarn go.**

-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**

-keep class com.my.vpn.util.HappCryptDecoder { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class com.my.vpn.vpn.** { *; }
-keep class com.my.vpn.data.local.PerServerBypassStorage { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
