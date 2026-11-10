# Add project specific ProGuard rules here.
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep data & domain models
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.network.** { *; }
-keep class com.example.discovery.** { *; }
