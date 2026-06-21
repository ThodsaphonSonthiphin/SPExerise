package com.spexerise.watchapp.data.health

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlin.math.sqrt

object PassiveHrvSession {
    private val hrBuffer = mutableListOf<Int>()
    private var appContext: Context? = null

    fun startNightlyMeasurement(context: Context, onResult: (rmssd: Float, restingHr: Int) -> Unit) {
        hrBuffer.clear()
        appContext = context.applicationContext
        val client = HealthServices.getClient(context).passiveMonitoringClient
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.HEART_RATE_BPM))
            .build()

        client.setPassiveListenerCallback(config, object : PassiveListenerCallback {
            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                dataPoints.getData(DataType.HEART_RATE_BPM).forEach { dataPoint ->
                    hrBuffer.add(dataPoint.value.toInt())
                }
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
        hrBuffer.clear()
        appContext = null
    }

    private fun computeRmssd(hrSamples: List<Int>): Float {
        val intervals = hrSamples.map { 60000.0 / it }
        val diffs = intervals.zipWithNext { a, b -> (b - a) * (b - a) }
        if (diffs.isEmpty()) return 0f
        return sqrt(diffs.average()).toFloat()
    }
}
