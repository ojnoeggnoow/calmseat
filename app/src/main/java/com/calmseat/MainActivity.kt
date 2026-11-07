package com.calmseat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmseat.ui.CalmSeatTheme
import com.calmseat.ui.CoachScreen
import com.calmseat.ui.ReportScreen

class MainActivity : ComponentActivity() {

    private val viewModel: CoachViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalmSeatTheme {
                CalmSeatApp(viewModel)
            }
        }
    }
}

@Composable
private fun CalmSeatApp(viewModel: CoachViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showReport by rememberSaveable { mutableStateOf(false) }

    // 다시 출발하면 리포트 닫기
    if (showReport && !state.canOpenReport) showReport = false

    if (showReport) {
        ReportScreen(state = state, onBack = { showReport = false })
    } else {
        CoachScreen(
            state = state,
            onCoach = viewModel::onCoach,
            onOpenReport = { showReport = true },
        )
    }
}
