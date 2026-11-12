# Mobile ProGuard rules
-keep class com.marketwear.mobile.** { *; }
-keep class com.marketwear.shared.model.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * extends kotlinx.serialization.internal.GeneratedSerializer { *; }
