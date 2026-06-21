package com.spexerise.watchapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.data.db.ReadinessSnapshot
import com.spexerise.watchapp.ui.exercise.ExerciseScreen
import com.spexerise.watchapp.ui.exercise.ExerciseViewModel
import com.spexerise.watchapp.ui.readiness.ReadinessScreen
import com.spexerise.watchapp.ui.theme.WatchAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val exerciseViewModel: ExerciseViewModel by viewModels()

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
                        ExerciseScreen(viewModel = exerciseViewModel)
                    }
                }
            }
        }
    }
}
