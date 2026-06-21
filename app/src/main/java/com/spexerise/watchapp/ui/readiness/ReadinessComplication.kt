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
