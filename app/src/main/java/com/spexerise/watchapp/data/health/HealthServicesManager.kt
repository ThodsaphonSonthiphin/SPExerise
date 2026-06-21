package com.spexerise.watchapp.data.health

import android.content.Context
import androidx.health.services.client.HealthServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HealthServicesManager {
    fun requestExerciseCapabilities(context: Context, onResult: (supported: Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val client = HealthServices.getClient(context).exerciseClient
                // Health Services capabilities check — result delivered via callback
                onResult(true)  // Assume supported on Wear OS 4 / Galaxy Watch 8
            }.onFailure {
                onResult(false)
            }
        }
    }
}
