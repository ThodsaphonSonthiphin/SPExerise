package com.spexerise.watchapp.ui.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.data.db.WorkoutRecord
import com.spexerise.watchapp.data.health.ExerciseRepository
import com.spexerise.watchapp.data.sync.WearableSyncManager
import com.spexerise.watchapp.domain.training.EpocCalculator
import com.spexerise.watchapp.domain.training.EpocState
import com.spexerise.watchapp.domain.training.TrainingEffectScore
import com.spexerise.watchapp.domain.training.Vo2MaxSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ExerciseUiState(
    val aerobicTE: Float = 0f,
    val anaerobicTE: Float = 0f,
    val aerobicLabel: String = "No benefit",
    val hrZone: Int? = null,
    val elapsedSeconds: Int = 0
)

class ExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val hrRest = 55   // TODO: read from user settings
    private val hrMax = 185   // TODO: 220 − age from user settings

    private var vo2Max = Vo2MaxSource.FALLBACK
    private var epocState = EpocState(0f, 0f)
    private val sessionStartMs = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState

    init {
        // Load VO2 max from DB on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            vo2Max = Vo2MaxSource.fromDao(AppDatabase.getInstance(context).vo2MaxDao())
        }
        // Start exercise session and collect HR updates
        viewModelScope.launch {
            ExerciseRepository.hrFlow(context).collect { hrBpm ->
                onHeartRateUpdate(hrBpm, 5)
            }
        }
    }

    fun onHeartRateUpdate(hrBpm: Int, deltaSeconds: Int = 5) {
        epocState = EpocCalculator.accumulate(epocState, hrBpm, hrRest, hrMax, vo2Max, deltaSeconds)
        val aerobic = TrainingEffectScore.aerobic(epocState, vo2Max)
        val anaerobic = TrainingEffectScore.anaerobic(epocState, vo2Max)
        val hrReserve = if (hrMax > hrRest) (hrBpm - hrRest).toFloat() / (hrMax - hrRest) else 0f
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

    override fun onCleared() {
        super.onCleared()
        // Save workout record to DB and sync to phone when session ends
        val durationSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000).toInt()
        val state = _uiState.value
        if (durationSeconds > 60) {  // only save sessions longer than 1 minute
            viewModelScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(context)
                val record = WorkoutRecord(
                    id = UUID.randomUUID().toString(),
                    startTimeMs = sessionStartMs,
                    durationSeconds = durationSeconds,
                    avgHrBpm = hrRest + ((hrMax - hrRest) * 0.70f).toInt(),
                    trainingStressScore = epocState.accumulatedEpoc,
                    aerobicTE = state.aerobicTE,
                    anaerobicTE = state.anaerobicTE,
                    peakEpoc = epocState.accumulatedEpoc
                )
                db.workoutDao().insert(record)
                WearableSyncManager.pushWorkout(context, record)
            }
        }
    }
}
