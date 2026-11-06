# Keep Wear OS complication data source services
-keep class com.marketwear.indices.** { *; }
-keep class androidx.wear.watchface.complications.** { *; }
-keepclassmembers class * extends androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService {
    <init>(...);
}
