# Keep Wear OS complication data source services
-keep class com.marketwear.indices.** { *; }
-keep class androidx.wear.watchface.complications.** { *; }
-keepclassmembers class * extends androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService {
    <init>(...);
}

# Kotlin Serialization & Shared models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.marketwear.shared.model.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * extends kotlinx.serialization.internal.GeneratedSerializer { *; }
