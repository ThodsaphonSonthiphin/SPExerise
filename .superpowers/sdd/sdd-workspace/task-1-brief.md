## Task 1: Project Setup — Wear OS Skeleton

**Files:**
- Create: `build.gradle.kts` (app module)
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/spexerise/watchapp/WatchApp.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/MainActivity.kt`

**Interfaces:**
- Produces: compilable Wear OS project; `MainActivity` shows "Hello Watch" on device

- [ ] **Step 1: Create Wear OS project in Android Studio**

  New Project → Wear OS → Empty Compose Activity. Package: `com.spexerise.watchapp`.

- [ ] **Step 2: Add dependencies to `app/build.gradle.kts`**

```kotlin
dependencies {
    // Compose for Wear OS
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.wear.compose:compose-navigation:1.3.0")

    // Health Services
    implementation("androidx.health:health-services-client:1.1.0-alpha03")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Wearable Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Samsung Health Sensor SDK (local AAR — download from developer.samsung.com/health)
    implementation(files("libs/samsung-health-sensor-sdk-1.5.aar"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

- [ ] **Step 3: Set permissions in `AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.BODY_SENSORS" />
    <uses-permission android:name="android.permission.BODY_SENSORS_BACKGROUND" />
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
    <uses-permission android:name="com.samsung.android.health.permission.READ" />
    <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />

    <uses-feature android:name="android.hardware.type.watch" />

    <application
        android:name=".WatchApp"
        android:theme="@style/Theme.SpExerise">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Write `MainActivity.kt` with "Hello Watch" Compose screen**

```kotlin
package com.spexerise.watchapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Text("Hello Watch")
            }
        }
    }
}
```

- [ ] **Step 5: Run on emulator or device, confirm "Hello Watch" appears**

- [ ] **Step 6: Commit**

```bash
git add app/
git commit -m "feat: wear os project skeleton with compose and health services deps"
```

---
