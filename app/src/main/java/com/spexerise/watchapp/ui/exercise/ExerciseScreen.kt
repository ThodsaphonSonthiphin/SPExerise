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

        Text(
            "Aerobic  %.1f".format(state.aerobicTE),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        TrainingEffectBar(progress = state.aerobicTE / 5f, color = Color(0xFF5FB4FF))

        Spacer(Modifier.height(4.dp))

        Text(
            "Anaerobic  %.1f".format(state.anaerobicTE),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
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
