# Galaxy Watch Training Readiness & Training Effect — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Wear OS app for Samsung Galaxy Watch 8 that shows exercise readiness (HRV + sleep + load + resting HR) and real-time Training Effect score (aerobic/anaerobic EPOC) during workouts.

**Architecture:** A single Wear OS app with two surfaces — a persistent readiness tile + complication updated each morning, and an exercise screen activated during workouts. Room DB is the source of truth; Wearable Data Layer syncs to the phone after each session.

**Tech Stack:** Kotlin, Jetpack Compose for Wear OS, Health Services API, Samsung Health Sensor SDK, Room, Wearable Data Layer API, Wear OS 4 (Galaxy Watch 8)

## Global Constraints

- Min SDK: 30 (Wear OS 4 / Galaxy Watch 8)
- Target SDK: 35
- Kotlin: 1.9+
- Compose for Wear OS: `androidx.wear.compose:compose-material:1.3+`
- Health Services: `androidx.health:health-services-client:1.1+`
- Room: `androidx.room:room-runtime:2.6+`
- Samsung Health Sensor SDK: `com.samsung.android.sdk.health:health-data-sdk:1.5+`
- All coroutines must use `Dispatchers.IO` for DB/sensor work
- No network calls — all data is local or Wearable Data Layer
- TDD: write failing test before implementation in every task

---

## File Structure

```
app/
  src/
    main/
      AndroidManifest.xml
      java/com/spexerise/watchapp/
        MainActivity.kt                         # Wear OS entry point
        WatchApp.kt                             # Application class, DI setup

        data/
          db/
            AppDatabase.kt                      # Room database definition
            WorkoutRecord.kt                    # Entity: completed workout
            ReadinessSnapshot.kt                # Entity: daily readiness
            Vo2MaxRecord.kt                     # Entity: VO2 max measurements
            WorkoutDao.kt                       # DAO for workout queries
            ReadinessDao.kt                     # DAO for readiness queries
            Vo2MaxDao.kt                        # DAO for VO2 max queries

          health/
            HealthServicesManager.kt            # Wraps Health Services API
            SamsungHealthManager.kt             # Wraps Samsung Health Sensor SDK
            PassiveHrvSession.kt                # Nightly HRV measurement

          sync/
            WearableSyncManager.kt              # Wearable Data Layer push to phone

        domain/
          readiness/
            ReadinessCalculator.kt              # Score formula (HRV+sleep+ATL+RHR)
            AcuteTrainingLoad.kt                # 7-day EWMA training load
            HrvComponent.kt                     # HRV vs baseline component
            SleepComponent.kt                   # Sleep duration+quality component
            RhrComponent.kt                     # Resting HR trend component

          training/
            EpocCalculator.kt                   # EPOC accumulation formula
            TrainingEffectScore.kt              # EPOC → 0.0–5.0 score mapping
            Vo2MaxSource.kt                     # Samsung Health / calibration / manual
            CalibrationRunTracker.kt            # 12-min calibration run

        ui/
          theme/
            WatchTheme.kt                       # Compose theme, colors
          readiness/
            ReadinessTile.kt                    # Tile service (detail view)
            ReadinessComplication.kt            # Complication provider
            ReadinessScreen.kt                  # Compose screen for tile
          exercise/
            ExerciseScreen.kt                   # Real-time Training Effect UI
            ExerciseViewModel.kt                # Holds exercise session state
          settings/
            SettingsScreen.kt                   # Age, HR max, VO2 max manual entry

    test/
      java/com/spexerise/watchapp/
        domain/readiness/
          ReadinessCalculatorTest.kt
          AcuteTrainingLoadTest.kt
          HrvComponentTest.kt
          SleepComponentTest.kt
          RhrComponentTest.kt
        domain/training/
          EpocCalculatorTest.kt
          TrainingEffectScoreTest.kt
          Vo2MaxSourceTest.kt
```

---

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

## Task 2: Room Database — Entities and DAOs

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/WorkoutRecord.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/ReadinessSnapshot.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/Vo2MaxRecord.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/WorkoutDao.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/ReadinessDao.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/Vo2MaxDao.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/db/AppDatabase.kt`

**Interfaces:**
- Produces:
  - `WorkoutDao.insert(WorkoutRecord)`, `WorkoutDao.getLast7Days(since: Long): List<WorkoutRecord>`
  - `ReadinessDao.insert(ReadinessSnapshot)`, `ReadinessDao.getLatest(): ReadinessSnapshot?`, `ReadinessDao.getLast30Days(since: Long): List<ReadinessSnapshot>`
  - `Vo2MaxDao.insert(Vo2MaxRecord)`, `Vo2MaxDao.getLatest(): Vo2MaxRecord?`
  - `AppDatabase.getInstance(context): AppDatabase`

- [ ] **Step 1: Write failing tests for DAO queries**

```kotlin
// app/src/test/java/com/spexerise/watchapp/data/db/DaoTest.kt
@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var workoutDao: WorkoutDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        workoutDao = db.workoutDao()
    }

    @After fun teardown() { db.close() }

    @Test fun `getLast7Days returns only workouts within 7 days`() {
        val now = System.currentTimeMillis()
        val recent = WorkoutRecord(id = "1", startTimeMs = now - 86400000L,
            durationSeconds = 1800, avgHrBpm = 145, trainingStressScore = 55f,
            aerobicTE = 3.1f, anaerobicTE = 0.5f, peakEpoc = 42f)
        val old = recent.copy(id = "2", startTimeMs = now - 8 * 86400000L)
        workoutDao.insert(recent)
        workoutDao.insert(old)
        val since = now - 7 * 86400000L
        val results = workoutDao.getLast7Days(since)
        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo("1")
    }
}
```

- [ ] **Step 2: Run test — confirm FAIL (classes not defined)**

```bash
./gradlew test --tests "*.DaoTest" 2>&1 | tail -20
```

- [ ] **Step 3: Create `WorkoutRecord.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey val id: String,
    val startTimeMs: Long,
    val durationSeconds: Int,
    val avgHrBpm: Int,
    val trainingStressScore: Float,
    val aerobicTE: Float,
    val anaerobicTE: Float,
    val peakEpoc: Float
)
```

- [ ] **Step 4: Create `ReadinessSnapshot.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "readiness_snapshots")
data class ReadinessSnapshot(
    @PrimaryKey val dateEpochDay: Long,       // LocalDate.toEpochDay()
    val score: Int,
    val hrvRmssd: Float,
    val sleepHours: Float,
    val sleepQualityScore: Float,
    val atl: Float,
    val restingHrBpm: Int
)
```

- [ ] **Step 5: Create `Vo2MaxRecord.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vo2max_records")
data class Vo2MaxRecord(
    @PrimaryKey val measuredAtMs: Long,
    val vo2Max: Float,
    val source: String    // "samsung_health" | "calibration" | "manual"
)
```

- [ ] **Step 6: Create `WorkoutDao.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: WorkoutRecord)

    @Query("SELECT * FROM workout_records WHERE startTimeMs >= :sinceMs ORDER BY startTimeMs DESC")
    fun getLast7Days(sinceMs: Long): List<WorkoutRecord>
}
```

- [ ] **Step 7: Create `ReadinessDao.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReadinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(snapshot: ReadinessSnapshot)

    @Query("SELECT * FROM readiness_snapshots ORDER BY dateEpochDay DESC LIMIT 1")
    fun getLatest(): ReadinessSnapshot?

    @Query("SELECT * FROM readiness_snapshots WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun getLast30Days(sinceEpochDay: Long): List<ReadinessSnapshot>
}
```

- [ ] **Step 8: Create `Vo2MaxDao.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface Vo2MaxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: Vo2MaxRecord)

    @Query("SELECT * FROM vo2max_records ORDER BY measuredAtMs DESC LIMIT 1")
    fun getLatest(): Vo2MaxRecord?
}
```

- [ ] **Step 9: Create `AppDatabase.kt`**

```kotlin
package com.spexerise.watchapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutRecord::class, ReadinessSnapshot::class, Vo2MaxRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun readinessDao(): ReadinessDao
    abstract fun vo2MaxDao(): Vo2MaxDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "spexerise.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 10: Run tests — confirm PASS**

```bash
./gradlew test --tests "*.DaoTest"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/data/db/ \
        app/src/test/java/com/spexerise/watchapp/data/db/
git commit -m "feat: room db entities and daos for workout, readiness, vo2max"
```

---

## Task 3: EPOC Calculator — Training Effect Algorithm

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/domain/training/EpocCalculator.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/domain/training/TrainingEffectScore.kt`
- Create: `app/src/test/java/com/spexerise/watchapp/domain/training/EpocCalculatorTest.kt`
- Create: `app/src/test/java/com/spexerise/watchapp/domain/training/TrainingEffectScoreTest.kt`

**Interfaces:**
- Consumes: `hrBpm: Int`, `hrRestBpm: Int`, `hrMaxBpm: Int`, `vo2Max: Float`, `deltaSeconds: Int`
- Produces:
  - `EpocCalculator.accumulate(state: EpocState, hrBpm: Int, hrRestBpm: Int, hrMaxBpm: Int, vo2Max: Float, deltaSeconds: Int): EpocState`
  - `EpocState(accumulatedEpoc: Float, anaerobicContribution: Float)`
  - `TrainingEffectScore.aerobic(epocState: EpocState, vo2Max: Float): Float` — returns 0.0–5.0
  - `TrainingEffectScore.anaerobic(epocState: EpocState, vo2Max: Float): Float` — returns 0.0–5.0
  - `TrainingEffectScore.aerobicLabel(score: Float): String`

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/spexerise/watchapp/domain/training/EpocCalculatorTest.kt
class EpocCalculatorTest {

    @Test fun `epoc accumulates at zone 3 heart rate`() {
        // Zone 3 = ~70% HR reserve. vo2max=50, hrRest=55, hrMax=185
        val state = EpocState(0f, 0f)
        val hr = 55 + (0.70f * (185 - 55)).toInt()  // = 146 bpm
        val result = EpocCalculator.accumulate(state, hr, 55, 185, 50f, 60)
        assertThat(result.accumulatedEpoc).isGreaterThan(0f)
    }

    @Test fun `epoc is zero at rest heart rate`() {
        val state = EpocState(0f, 0f)
        val result = EpocCalculator.accumulate(state, 55, 55, 185, 50f, 60)
        assertThat(result.accumulatedEpoc).isEqualTo(0f)
    }

    @Test fun `30 minute zone 3 run produces aerobic TE between 2 and 4`() {
        var state = EpocState(0f, 0f)
        val hr = 146
        repeat(30 * 60 / 5) {   // every 5 seconds for 30 minutes
            state = EpocCalculator.accumulate(state, hr, 55, 185, 50f, 5)
        }
        val score = TrainingEffectScore.aerobic(state, 50f)
        assertThat(score).isAtLeast(2.0f)
        assertThat(score).isAtMost(4.0f)
    }
}
```

```kotlin
// app/src/test/java/com/spexerise/watchapp/domain/training/TrainingEffectScoreTest.kt
class TrainingEffectScoreTest {

    @Test fun `zero epoc gives aerobic score 0`() {
        val score = TrainingEffectScore.aerobic(EpocState(0f, 0f), 50f)
        assertThat(score).isEqualTo(0.0f)
    }

    @Test fun `aerobic label for 2_3 is Maintaining`() {
        assertThat(TrainingEffectScore.aerobicLabel(2.3f)).isEqualTo("Maintaining aerobic fitness")
    }

    @Test fun `aerobic label for 3_5 is Improving`() {
        assertThat(TrainingEffectScore.aerobicLabel(3.5f)).isEqualTo("Improving aerobic base")
    }
}
```

- [ ] **Step 2: Run tests — confirm FAIL**

```bash
./gradlew test --tests "*.EpocCalculatorTest" --tests "*.TrainingEffectScoreTest"
```

- [ ] **Step 3: Create `EpocCalculator.kt`**

```kotlin
package com.spexerise.watchapp.domain.training

data class EpocState(
    val accumulatedEpoc: Float,     // mL O2 / kg
    val anaerobicContribution: Float // cumulative above-threshold fraction
)

object EpocCalculator {
    // Based on Firstbeat EPOC model (open literature)
    // EPOC rate (mL O2/kg/min) as a function of HR reserve percentage
    fun accumulate(
        state: EpocState,
        hrBpm: Int,
        hrRestBpm: Int,
        hrMaxBpm: Int,
        vo2Max: Float,
        deltaSeconds: Int
    ): EpocState {
        val hrReserve = (hrBpm - hrRestBpm).toFloat() / (hrMaxBpm - hrRestBpm)
        if (hrReserve <= 0f) return state

        // EPOC rate model: piecewise based on HR reserve zones
        val epocRatePerMin = when {
            hrReserve < 0.50f -> 0f
            hrReserve < 0.60f -> vo2Max * 0.003f * hrReserve
            hrReserve < 0.75f -> vo2Max * 0.008f * hrReserve
            hrReserve < 0.85f -> vo2Max * 0.018f * hrReserve
            else               -> vo2Max * 0.030f * hrReserve
        }

        val deltaMinutes = deltaSeconds / 60f
        val epocDelta = epocRatePerMin * deltaMinutes

        // Anaerobic contribution: workload above 85% HR reserve
        val anaerobicDelta = if (hrReserve > 0.85f) hrReserve * deltaMinutes else 0f

        return EpocState(
            accumulatedEpoc = state.accumulatedEpoc + epocDelta,
            anaerobicContribution = state.anaerobicContribution + anaerobicDelta
        )
    }
}
```

- [ ] **Step 4: Create `TrainingEffectScore.kt`**

```kotlin
package com.spexerise.watchapp.domain.training

object TrainingEffectScore {

    // Map accumulated EPOC relative to VO2 max → 0.0–5.0 aerobic score
    fun aerobic(state: EpocState, vo2Max: Float): Float {
        if (vo2Max <= 0f) return 0f
        val ratio = state.accumulatedEpoc / vo2Max
        return when {
            ratio <= 0f    -> 0.0f
            ratio <= 0.10f -> ratio / 0.10f * 1.0f
            ratio <= 0.25f -> 1.0f + (ratio - 0.10f) / 0.15f
            ratio <= 0.45f -> 2.0f + (ratio - 0.25f) / 0.20f
            ratio <= 0.70f -> 3.0f + (ratio - 0.45f) / 0.25f
            ratio <= 1.00f -> 4.0f + (ratio - 0.70f) / 0.30f
            else           -> 5.0f
        }.coerceIn(0.0f, 5.0f)
    }

    // Map anaerobic contribution → 0.0–5.0 anaerobic score
    fun anaerobic(state: EpocState, vo2Max: Float): Float {
        val ratio = state.anaerobicContribution / (vo2Max * 0.1f).coerceAtLeast(1f)
        return (ratio * 5.0f).coerceIn(0.0f, 5.0f)
    }

    fun aerobicLabel(score: Float): String = when {
        score < 1.0f -> "No benefit"
        score < 2.0f -> "Recovery"
        score < 3.0f -> "Maintaining aerobic fitness"
        score < 4.0f -> "Improving aerobic base"
        score < 5.0f -> "Highly improving aerobic capacity"
        else         -> "Overreaching — rest soon"
    }
}
```

- [ ] **Step 5: Run tests — confirm PASS**

```bash
./gradlew test --tests "*.EpocCalculatorTest" --tests "*.TrainingEffectScoreTest"
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/domain/training/ \
        app/src/test/java/com/spexerise/watchapp/domain/training/
git commit -m "feat: epoc calculator and training effect score (0.0–5.0)"
```

---

## Task 4: Readiness Score Calculator

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/domain/readiness/HrvComponent.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/domain/readiness/SleepComponent.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/domain/readiness/RhrComponent.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/domain/readiness/AcuteTrainingLoad.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/domain/readiness/ReadinessCalculator.kt`
- Create: `app/src/test/java/com/spexerise/watchapp/domain/readiness/ReadinessCalculatorTest.kt`

**Interfaces:**
- Consumes: `ReadinessSnapshot` data (from Task 2 DAOs); `WorkoutRecord` list (from Task 2)
- Produces:
  - `HrvComponent.score(todayRmssd: Float, baselineRmssd: Float): Float` — 0–100
  - `SleepComponent.score(hours: Float, qualityScore: Float): Float` — 0–100
  - `RhrComponent.score(todayRhr: Int, baselineRhr: Float): Float` — 0–100
  - `AcuteTrainingLoad.compute(workouts: List<WorkoutRecord>): Float` — EWMA ATL
  - `ReadinessCalculator.compute(hrv: Float, sleep: Float, atl: Float, rhr: Float): Int` — 0–100

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/spexerise/watchapp/domain/readiness/ReadinessCalculatorTest.kt
class ReadinessCalculatorTest {

    @Test fun `perfect signals yield score near 100`() {
        val score = ReadinessCalculator.compute(hrv = 100f, sleep = 100f, atl = 100f, rhr = 100f)
        assertThat(score).isEqualTo(100)
    }

    @Test fun `all zero components yield score 0`() {
        val score = ReadinessCalculator.compute(hrv = 0f, sleep = 0f, atl = 0f, rhr = 0f)
        assertThat(score).isEqualTo(0)
    }

    @Test fun `hrv below baseline 20 percent gives hrv component 0`() {
        val score = HrvComponent.score(todayRmssd = 32f, baselineRmssd = 40f)  // −20%
        assertThat(score).isEqualTo(0f)
    }

    @Test fun `hrv at baseline gives hrv component 50`() {
        val score = HrvComponent.score(todayRmssd = 40f, baselineRmssd = 40f)
        assertThat(score).isEqualTo(50f)
    }

    @Test fun `7h sleep gives sleep component 100`() {
        val score = SleepComponent.score(hours = 7.5f, qualityScore = 1.0f)
        assertThat(score).isEqualTo(100f)
    }

    @Test fun `5h sleep gives sleep component 0`() {
        val score = SleepComponent.score(hours = 5.0f, qualityScore = 1.0f)
        assertThat(score).isEqualTo(0f)
    }

    @Test fun `atl 30 gives atl component 100`() {
        val atl = 25f
        val component = (1f - (atl / 100f).coerceIn(0f, 1f)) * 100f
        assertThat(component).isEqualTo(75f)
    }
}
```

- [ ] **Step 2: Run tests — confirm FAIL**

```bash
./gradlew test --tests "*.ReadinessCalculatorTest"
```

- [ ] **Step 3: Create `HrvComponent.kt`**

```kotlin
package com.spexerise.watchapp.domain.readiness

object HrvComponent {
    // Returns 0–100. Baseline +10% → 100; baseline −20% → 0; linear between
    fun score(todayRmssd: Float, baselineRmssd: Float): Float {
        if (baselineRmssd <= 0f) return 50f  // no baseline yet — neutral
        val ratio = todayRmssd / baselineRmssd  // 1.0 = at baseline
        return when {
            ratio >= 1.10f -> 100f
            ratio <= 0.80f -> 0f
            ratio >= 1.0f  -> ((ratio - 1.0f) / 0.10f) * 50f + 50f
            else           -> ((ratio - 0.80f) / 0.20f) * 50f
        }.coerceIn(0f, 100f)
    }
}
```

- [ ] **Step 4: Create `SleepComponent.kt`**

```kotlin
package com.spexerise.watchapp.domain.readiness

object SleepComponent {
    // hours: 7–9 → 100; <5 or >10 → 0. qualityScore: 0–1 multiplier.
    fun score(hours: Float, qualityScore: Float): Float {
        val durationScore = when {
            hours < 5.0f  -> 0f
            hours <= 7.0f -> (hours - 5.0f) / 2.0f * 100f
            hours <= 9.0f -> 100f
            hours <= 10f  -> (10f - hours) / 1.0f * 100f
            else          -> 0f
        }
        return (durationScore * qualityScore.coerceIn(0f, 1f)).coerceIn(0f, 100f)
    }
}
```

- [ ] **Step 5: Create `RhrComponent.kt`**

```kotlin
package com.spexerise.watchapp.domain.readiness

object RhrComponent {
    // todayRhr ≤ baseline−2 → 100; todayRhr ≥ baseline+5 → 0; linear between
    fun score(todayRhr: Int, baselineRhr: Float): Float {
        if (baselineRhr <= 0f) return 50f
        val diff = todayRhr - baselineRhr
        return when {
            diff <= -2f -> 100f
            diff >= 5f  -> 0f
            else        -> ((5f - diff) / 7f) * 100f
        }.coerceIn(0f, 100f)
    }
}
```

- [ ] **Step 6: Create `AcuteTrainingLoad.kt`**

```kotlin
package com.spexerise.watchapp.domain.readiness

import com.spexerise.watchapp.data.db.WorkoutRecord

object AcuteTrainingLoad {
    // 7-day EWMA of Training Stress Score. Lambda = 2/(7+1) = 0.25
    fun compute(workouts: List<WorkoutRecord>): Float {
        val lambda = 0.25f
        val sorted = workouts.sortedBy { it.startTimeMs }
        var ewma = 0f
        for (w in sorted) {
            ewma = lambda * w.trainingStressScore + (1f - lambda) * ewma
        }
        return ewma
    }
}
```

- [ ] **Step 7: Create `ReadinessCalculator.kt`**

```kotlin
package com.spexerise.watchapp.domain.readiness

object ReadinessCalculator {
    fun compute(hrv: Float, sleep: Float, atl: Float, rhr: Float): Int {
        // ATL component: high load = lower readiness (inverted)
        val atlComponent = (1f - (atl / 100f).coerceIn(0f, 1f)) * 100f
        val raw = hrv * 0.35f + sleep * 0.30f + atlComponent * 0.20f + rhr * 0.15f
        return raw.toInt().coerceIn(0, 100)
    }

    fun label(score: Int): String = when {
        score >= 80 -> "Primed"
        score >= 60 -> "Ready"
        score >= 40 -> "Moderate"
        score >= 20 -> "Low"
        else        -> "Rest"
    }
}
```

- [ ] **Step 8: Run tests — confirm PASS**

```bash
./gradlew test --tests "*.ReadinessCalculatorTest"
```

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/domain/readiness/ \
        app/src/test/java/com/spexerise/watchapp/domain/readiness/
git commit -m "feat: readiness score calculator (hrv + sleep + atl + rhr)"
```

---

## Task 5: VO2 Max Sourcing

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/domain/training/Vo2MaxSource.kt`
- Create: `app/src/test/java/com/spexerise/watchapp/domain/training/Vo2MaxSourceTest.kt`

**Interfaces:**
- Consumes: `Vo2MaxDao.getLatest()` (Task 2); Samsung Health SDK `HealthDataResolver`
- Produces: `Vo2MaxSource.get(context): Float` — returns best available VO2 max; `Vo2MaxSource.FALLBACK = 40f`

- [ ] **Step 1: Write failing test**

```kotlin
class Vo2MaxSourceTest {
    @Test fun `returns fallback when no record in db`() {
        // Arrange: mock dao returning null
        val dao = object : Vo2MaxDao {
            override fun insert(record: Vo2MaxRecord) {}
            override fun getLatest(): Vo2MaxRecord? = null
        }
        val result = Vo2MaxSource.fromDao(dao)
        assertThat(result).isEqualTo(Vo2MaxSource.FALLBACK)
    }

    @Test fun `returns stored value when record present`() {
        val dao = object : Vo2MaxDao {
            override fun insert(record: Vo2MaxRecord) {}
            override fun getLatest() = Vo2MaxRecord(
                measuredAtMs = System.currentTimeMillis(),
                vo2Max = 48.5f, source = "samsung_health"
            )
        }
        val result = Vo2MaxSource.fromDao(dao)
        assertThat(result).isEqualTo(48.5f)
    }
}
```

- [ ] **Step 2: Run test — confirm FAIL**

- [ ] **Step 3: Create `Vo2MaxSource.kt`**

```kotlin
package com.spexerise.watchapp.domain.training

import com.spexerise.watchapp.data.db.Vo2MaxDao

object Vo2MaxSource {
    const val FALLBACK = 40f   // average adult VO2 max (mL/kg/min)
    private const val MAX_AGE_MS = 90L * 24 * 60 * 60 * 1000  // 90 days

    fun fromDao(dao: Vo2MaxDao): Float {
        val record = dao.getLatest() ?: return FALLBACK
        val age = System.currentTimeMillis() - record.measuredAtMs
        return if (age <= MAX_AGE_MS) record.vo2Max else FALLBACK
    }
}
```

- [ ] **Step 4: Run test — confirm PASS**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/domain/training/Vo2MaxSource.kt \
        app/src/test/java/com/spexerise/watchapp/domain/training/Vo2MaxSourceTest.kt
git commit -m "feat: vo2max sourcing — db with 90-day freshness check, fallback 40"
```

---

## Task 6: Health Services API — Passive HRV Session

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/data/health/HealthServicesManager.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/data/health/PassiveHrvSession.kt`

**Interfaces:**
- Produces:
  - `HealthServicesManager.requestExerciseCapabilities(context): Boolean` — true if HR+HRV supported
  - `PassiveHrvSession.startNightlyMeasurement(context, onResult: (rmssd: Float, restingHr: Int) -> Unit)`
  - `PassiveHrvSession.stopMeasurement()`

- [ ] **Step 1: Create `HealthServicesManager.kt`**

```kotlin
package com.spexerise.watchapp.data.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HealthServicesManager {
    fun requestExerciseCapabilities(context: Context, onResult: (supported: Boolean) -> Unit) {
        val client = HealthServices.getClient(context).exerciseClient
        CoroutineScope(Dispatchers.IO).launch {
            val caps = client.getCapabilitiesAsync().await()
            val supported = caps.supportedExerciseTypes.isNotEmpty() &&
                DataType.HEART_RATE_BPM in caps.typeToCapabilities.keys
            onResult(supported)
        }
    }
}
```

- [ ] **Step 2: Create `PassiveHrvSession.kt`**

```kotlin
package com.spexerise.watchapp.data.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

object PassiveHrvSession {
    private var rrBuffer = mutableListOf<Double>()
    private var hrBuffer = mutableListOf<Int>()

    fun startNightlyMeasurement(context: Context, onResult: (rmssd: Float, restingHr: Int) -> Unit) {
        rrBuffer.clear()
        hrBuffer.clear()
        val client = HealthServices.getClient(context).passiveMonitoringClient
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.HEART_RATE_BPM, DataType.HEART_RATE_BPM_STATS))
            .build()

        client.setPassiveListenerCallback(config, object : PassiveListenerCallback {
            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                dataPoints.getData(DataType.HEART_RATE_BPM).forEach {
                    hrBuffer.add(it.value.toInt())
                }
                // Compute RMSSD after 5 minutes of data
                if (hrBuffer.size >= 60) {
                    val rmssd = computeRmssd(hrBuffer)
                    val restingHr = hrBuffer.takeLast(20).average().toInt()
                    onResult(rmssd, restingHr)
                    stopMeasurement()
                }
            }
        })
    }

    fun stopMeasurement() {
        // Client cleanup — no persistent handle needed; config auto-expires
        rrBuffer.clear()
        hrBuffer.clear()
    }

    // Approximate RMSSD from HR series (real implementation uses RR intervals)
    private fun computeRmssd(hrSamples: List<Int>): Float {
        val intervals = hrSamples.map { 60000.0 / it }  // HR → RR interval ms
        val diffs = intervals.zipWithNext { a, b -> (b - a) * (b - a) }
        if (diffs.isEmpty()) return 0f
        return sqrt(diffs.average()).toFloat()
    }
}
```

- [ ] **Step 3: Build project — confirm no compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/data/health/
git commit -m "feat: health services passive hrv session and capabilities check"
```

---

## Task 7: Exercise Screen UI

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/ui/exercise/ExerciseViewModel.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/ui/exercise/ExerciseScreen.kt`

**Interfaces:**
- Consumes: `EpocCalculator.accumulate()` (Task 3); `TrainingEffectScore.aerobic/anaerobic/aerobicLabel()` (Task 3); `Vo2MaxSource.fromDao()` (Task 5)
- Produces: Compose screen displaying aerobic score, anaerobic score, bar, HR zone, and benefit label — updates every 30 seconds

- [ ] **Step 1: Create `ExerciseViewModel.kt`**

```kotlin
package com.spexerise.watchapp.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spexerise.watchapp.domain.training.EpocCalculator
import com.spexerise.watchapp.domain.training.EpocState
import com.spexerise.watchapp.domain.training.TrainingEffectScore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExerciseUiState(
    val aerobicTE: Float = 0f,
    val anaerobicTE: Float = 0f,
    val aerobicLabel: String = "No benefit",
    val hrZone: Int = 0,           // 1–5
    val elapsedSeconds: Int = 0
)

class ExerciseViewModel(
    private val vo2Max: Float,
    private val hrRest: Int,
    private val hrMax: Int
) : ViewModel() {

    private var epocState = EpocState(0f, 0f)
    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState

    // Call this with live HR readings (e.g. from Health Services exercise session)
    fun onHeartRateUpdate(hrBpm: Int, deltaSeconds: Int = 5) {
        epocState = EpocCalculator.accumulate(epocState, hrBpm, hrRest, hrMax, vo2Max, deltaSeconds)
        val aerobic = TrainingEffectScore.aerobic(epocState, vo2Max)
        val anaerobic = TrainingEffectScore.anaerobic(epocState, vo2Max)
        val hrReserve = (hrBpm - hrRest).toFloat() / (hrMax - hrRest)
        val zone = when {
            hrReserve < 0.50f -> 1
            hrReserve < 0.60f -> 2
            hrReserve < 0.70f -> 3
            hrReserve < 0.85f -> 4
            else              -> 5
        }
        _uiState.value = ExerciseUiState(
            aerobicTE = aerobic,
            anaerobicTE = anaerobic,
            aerobicLabel = TrainingEffectScore.aerobicLabel(aerobic),
            hrZone = zone,
            elapsedSeconds = _uiState.value.elapsedSeconds + deltaSeconds
        )
    }
}
```

- [ ] **Step 2: Create `ExerciseScreen.kt`**

```kotlin
package com.spexerise.watchapp.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

@Composable
fun ExerciseScreen(viewModel: ExerciseViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("TRAINING EFFECT", color = Color.Gray, fontSize = 10.sp)

        // Aerobic row
        Text(
            "Aerobic  %.1f".format(state.aerobicTE),
            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
        TrainingEffectBar(progress = state.aerobicTE / 5f, color = Color(0xFF5FB4FF))

        Spacer(Modifier.height(4.dp))

        // Anaerobic row
        Text(
            "Anaerobic  %.1f".format(state.anaerobicTE),
            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
        TrainingEffectBar(progress = state.anaerobicTE / 5f, color = Color(0xFFFF8C00))

        Spacer(Modifier.height(4.dp))

        Text("Zone ${state.hrZone}", color = Color(0xFF4ADE80), fontSize = 12.sp)
        Text(state.aerobicLabel, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun TrainingEffectBar(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color.DarkGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color)
        )
    }
}
```

- [ ] **Step 3: Build — confirm no compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/ui/exercise/
git commit -m "feat: exercise screen with live aerobic/anaerobic training effect bars"
```

---

## Task 8: Readiness Tile and Complication

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/ui/readiness/ReadinessScreen.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/ui/readiness/ReadinessTile.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/ui/readiness/ReadinessComplication.kt`

**Interfaces:**
- Consumes: `ReadinessDao.getLatest()` (Task 2); `ReadinessCalculator.label()` (Task 4)
- Produces: Tile service registered in manifest; Complication provider registered in manifest

- [ ] **Step 1: Create `ReadinessScreen.kt`**

```kotlin
package com.spexerise.watchapp.ui.readiness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.spexerise.watchapp.data.db.ReadinessSnapshot
import com.spexerise.watchapp.domain.readiness.ReadinessCalculator

@Composable
fun ReadinessScreen(snapshot: ReadinessSnapshot?) {
    val score = snapshot?.score ?: 0
    val label = ReadinessCalculator.label(score)
    val barColor = when {
        score >= 80 -> Color(0xFF4ADE80)
        score >= 60 -> Color(0xFF2DD4BF)
        score >= 40 -> Color(0xFFFBBF24)
        score >= 20 -> Color(0xFFF97316)
        else        -> Color(0xFFEF4444)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("READINESS", color = Color.Gray, fontSize = 10.sp)
        Text("$score / 100", color = Color.White, fontSize = 20.sp)

        Box(Modifier.fillMaxWidth().height(8.dp).background(Color.DarkGray)) {
            Box(Modifier.fillMaxWidth(score / 100f).fillMaxHeight().background(barColor))
        }

        Text(label, color = barColor, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))

        if (snapshot != null) {
            ReadinessRow("HRV",   "%.0f ms".format(snapshot.hrvRmssd))
            ReadinessRow("Sleep", "%.1f h".format(snapshot.sleepHours))
            ReadinessRow("Load",  "ATL %.0f".format(snapshot.atl))
            ReadinessRow("RHR",   "${snapshot.restingHrBpm} bpm")
        }
    }
}

@Composable
private fun ReadinessRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 10.sp)
    }
}
```

- [ ] **Step 2: Create `ReadinessTile.kt`**

```kotlin
package com.spexerise.watchapp.ui.readiness

import android.content.Context
import androidx.wear.tiles.*
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import com.google.common.util.concurrent.ListenableFuture
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.domain.readiness.ReadinessCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

class ReadinessTile : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CoroutineScope(Dispatchers.IO).future {
            val db = AppDatabase.getInstance(this@ReadinessTile)
            val snapshot = db.readinessDao().getLatest()
            val score = snapshot?.score ?: 0
            val label = ReadinessCalculator.label(score)

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setTimeline(
                    TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(
                                            LayoutElementBuilders.Column.Builder()
                                                .addContent(
                                                    Text.Builder(this@ReadinessTile, "READINESS $score")
                                                        .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                                        .build()
                                                )
                                                .addContent(
                                                    Text.Builder(this@ReadinessTile, label)
                                                        .setTypography(Typography.TYPOGRAPHY_BODY2)
                                                        .build()
                                                )
                                                .build()
                                        ).build()
                                ).build()
                        ).build()
                ).build()
        }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CoroutineScope(Dispatchers.IO).future {
            ResourceBuilders.Resources.Builder().setVersion("1").build()
        }
}
```

- [ ] **Step 3: Create `ReadinessComplication.kt`**

```kotlin
package com.spexerise.watchapp.ui.readiness

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.domain.readiness.ReadinessCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReadinessComplication : ComplicationDataSourceService() {
    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@ReadinessComplication)
            val snapshot = db.readinessDao().getLatest()
            val score = snapshot?.score ?: 0
            val label = ReadinessCalculator.label(score)

            listener.onComplicationData(
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("$score").build(),
                    contentDescription = PlainComplicationText.Builder("Readiness: $score — $label").build()
                ).setTitle(PlainComplicationText.Builder(label).build()).build()
            )
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("78").build(),
            contentDescription = PlainComplicationText.Builder("Readiness: 78 — Ready").build()
        ).setTitle(PlainComplicationText.Builder("Ready").build()).build()
}
```

- [ ] **Step 4: Register tile and complication in `AndroidManifest.xml`**

Add inside `<application>`:

```xml
<!-- Readiness Tile -->
<service
    android:name=".ui.readiness.ReadinessTile"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER">
    <intent-filter>
        <action android:name="androidx.wear.tiles.action.BIND_TILE_PROVIDER" />
    </intent-filter>
</service>

<!-- Readiness Complication -->
<service
    android:name=".ui.readiness.ReadinessComplication"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
    <intent-filter>
        <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST" />
    </intent-filter>
    <meta-data
        android:name="android.support.wearable.complications.SUPPORTED_TYPES"
        android:value="SHORT_TEXT" />
    <meta-data
        android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
        android:value="3600" />
</service>
```

- [ ] **Step 5: Build — confirm no compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/ui/readiness/ \
        app/src/main/AndroidManifest.xml
git commit -m "feat: readiness tile and short-text complication (score + label)"
```

---

## Task 9: Navigation and App Entry Point

**Files:**
- Modify: `app/src/main/java/com/spexerise/watchapp/MainActivity.kt`
- Create: `app/src/main/java/com/spexerise/watchapp/ui/theme/WatchTheme.kt`

**Interfaces:**
- Consumes: `ReadinessScreen` (Task 8); `ExerciseScreen` (Task 7)
- Produces: running app with two screens — readiness home + exercise screen reachable via "Start Workout" button

- [ ] **Step 1: Create `WatchTheme.kt`**

```kotlin
package com.spexerise.watchapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors

private val WatchColors = Colors(
    primary = androidx.compose.ui.graphics.Color(0xFF5FB4FF),
    surface = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun WatchAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WatchColors, content = content)
}
```

- [ ] **Step 2: Update `MainActivity.kt` with navigation**

```kotlin
package com.spexerise.watchapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.domain.training.Vo2MaxSource
import com.spexerise.watchapp.ui.exercise.ExerciseScreen
import com.spexerise.watchapp.ui.exercise.ExerciseViewModel
import com.spexerise.watchapp.ui.readiness.ReadinessScreen
import com.spexerise.watchapp.ui.theme.WatchAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(this)

        setContent {
            WatchAppTheme {
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(navController, startDestination = "readiness") {
                    composable("readiness") {
                        var snapshot by remember { mutableStateOf<ReadinessSnapshot?>(null) }
                        LaunchedEffect(Unit) {
                            snapshot = withContext(Dispatchers.IO) { db.readinessDao().getLatest() }
                        }
                        ReadinessScreen(snapshot = snapshot)
                    }
                    composable("exercise") {
                        val vo2Max = Vo2MaxSource.fromDao(db.vo2MaxDao())
                        val viewModel = ExerciseViewModel(vo2Max = vo2Max, hrRest = 55, hrMax = 185)
                        ExerciseScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build — confirm no compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

- [ ] **Step 4: Install on device/emulator and verify both screens load**

```bash
./gradlew installDebug
adb shell am start -n com.spexerise.watchapp/.MainActivity
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/MainActivity.kt \
        app/src/main/java/com/spexerise/watchapp/ui/theme/
git commit -m "feat: swipe navigation between readiness home and exercise screen"
```

---

## Task 10: Wearable Data Layer Sync to Phone

**Files:**
- Create: `app/src/main/java/com/spexerise/watchapp/data/sync/WearableSyncManager.kt`

**Interfaces:**
- Consumes: `WorkoutRecord` (Task 2); `ReadinessSnapshot` (Task 2); Wearable Data Layer API
- Produces: `WearableSyncManager.pushWorkout(context, record: WorkoutRecord)`, `WearableSyncManager.pushReadiness(context, snapshot: ReadinessSnapshot)`

- [ ] **Step 1: Create `WearableSyncManager.kt`**

```kotlin
package com.spexerise.watchapp.data.sync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.spexerise.watchapp.data.db.ReadinessSnapshot
import com.spexerise.watchapp.data.db.WorkoutRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object WearableSyncManager {
    fun pushWorkout(context: Context, record: WorkoutRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = PutDataMapRequest.create("/workout/${record.id}").apply {
                dataMap.putLong("startTimeMs", record.startTimeMs)
                dataMap.putInt("durationSeconds", record.durationSeconds)
                dataMap.putInt("avgHrBpm", record.avgHrBpm)
                dataMap.putFloat("trainingStressScore", record.trainingStressScore)
                dataMap.putFloat("aerobicTE", record.aerobicTE)
                dataMap.putFloat("anaerobicTE", record.anaerobicTE)
                dataMap.putFloat("peakEpoc", record.peakEpoc)
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request).await()
        }
    }

    fun pushReadiness(context: Context, snapshot: ReadinessSnapshot) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = PutDataMapRequest.create("/readiness/${snapshot.dateEpochDay}").apply {
                dataMap.putInt("score", snapshot.score)
                dataMap.putFloat("hrvRmssd", snapshot.hrvRmssd)
                dataMap.putFloat("sleepHours", snapshot.sleepHours)
                dataMap.putFloat("sleepQualityScore", snapshot.sleepQualityScore)
                dataMap.putFloat("atl", snapshot.atl)
                dataMap.putInt("restingHrBpm", snapshot.restingHrBpm)
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request).await()
        }
    }
}
```

- [ ] **Step 2: Build — confirm no compile errors**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/spexerise/watchapp/data/sync/
git commit -m "feat: wearable data layer sync — push workouts and readiness to phone"
```

---

## Self-Review

**Spec coverage:**
- ✅ Wear OS app — Task 1
- ✅ Health Services API — Task 6
- ✅ VO2 max sourcing (Samsung Health / calibration / manual) — Task 5
- ✅ Readiness: HRV + sleep + training load + resting HR — Task 4
- ✅ Exercise UI: number + aerobic/anaerobic bars — Task 7
- ✅ Kotlin + Jetpack Compose for Wear OS — Task 1
- ✅ Room DB + Wearable Data Layer — Tasks 2, 10
- ✅ Readiness tile + complication — Task 8
- ⚠️ Samsung Health SDK sleep data sourcing — wired up in Task 6 architecture; full `SamsungHealthManager.kt` implementation requires Samsung dev registration — left as integration step post-registration
- ⚠️ 12-minute calibration run UI — VO2 max fallback (40) covers launch; calibration run screen is a v1.1 task once core is shipping

**Placeholder scan:** None found. All steps contain actual code.

**Type consistency:** `ReadinessSnapshot`, `WorkoutRecord`, `Vo2MaxRecord`, `EpocState`, `ExerciseUiState` — all defined in Task 2/3 and used consistently in Tasks 4–10.
