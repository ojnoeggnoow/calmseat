package com.calmseat

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    // 속도는 사용자가 허용해야 읽을 수 있음. 허용되면 AaosCarSignals가 알아서 다시 구독함
    private val askSpeedPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isCar = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        if (isCar && checkSelfPermission(CAR_SPEED) != PackageManager.PERMISSION_GRANTED) {
            askSpeedPermission.launch(CAR_SPEED)
        }
        setContent {
            CalmSeatTheme {
                CalmSeatApp(viewModel)
            }
        }
    }

    private companion object {
        const val CAR_SPEED = "android.car.permission.CAR_SPEED"
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
            onCoach = { prompt -> viewModel.onCoach(prompt) },
            onOpenReport = { showReport = true },
            showDemoButton = viewModel.canToggleDemo,
            onToggleDemo = { viewModel.toggleDemo() },
        )
    }
}
