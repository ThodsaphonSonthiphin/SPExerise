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
            ReadinessRow("HRV", "%.0f ms".format(snapshot.hrvRmssd))
            ReadinessRow("Sleep", "%.1f h".format(snapshot.sleepHours))
            ReadinessRow("Load", "ATL %.0f".format(snapshot.atl))
            ReadinessRow("RHR", "${snapshot.restingHrBpm} bpm")
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
