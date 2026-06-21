package com.spexerise.watchapp.ui.exercise

import androidx.lifecycle.ViewModel
import com.spexerise.watchapp.domain.training.EpocCalculator
import com.spexerise.watchapp.domain.training.EpocState
import com.spexerise.watchapp.domain.training.TrainingEffectScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ExerciseUiState(
    val aerobicTE: Float = 0f,
    val anaerobicTE: Float = 0f,
    val aerobicLabel: String = "No benefit",
    val hrZone: Int = 0,
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
}
