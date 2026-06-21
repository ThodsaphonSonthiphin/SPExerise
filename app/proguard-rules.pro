# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Wear OS classes
-keep class androidx.wear.** { *; }

# Keep Samsung Health SDK
-keep class com.samsung.android.sdk.** { *; }

# Keep Health Services
-keep class androidx.health.** { *; }
