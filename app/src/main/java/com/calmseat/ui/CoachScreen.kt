package com.calmseat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmseat.CoachPrompt
import com.calmseat.CoachUiState
import com.calmseat.vehicle.Gear
import com.calmseat.vehicle.TurnSignal
import com.calmseat.vehicle.VehicleSnapshot

// 메인. 왼쪽 상황 카드, 오른쪽 버튼. 주행 중엔 이 화면만
@Composable
fun CoachScreen(
    state: CoachUiState,
    onCoach: (CoachPrompt) -> Unit,
    onOpenReport: () -> Unit,
    showDemoButton: Boolean = false,
    onToggleDemo: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmColors.Background)
            .padding(20.dp)
    ) {
        TopBar(state)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.weight(1f)) {
            SituationCard(state, modifier = Modifier.weight(1.3f).fillMaxHeight())
            Spacer(Modifier.width(16.dp))
            CoachButtons(onCoach, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(12.dp))
        BottomBar(state, onOpenReport, showDemoButton, onToggleDemo)
    }
}

@Composable
private fun TopBar(state: CoachUiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("차분한 조수석", color = CalmColors.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        Badge(state.sourceLabel, if (state.sourceLabel == "차량 연결") CalmColors.Good else CalmColors.Warn)
        Spacer(Modifier.width(8.dp))
        Badge(state.voiceLabel, CalmColors.TextDim)
        Spacer(Modifier.weight(1f))
        Text("동승자용", color = CalmColors.TextDim, fontSize = 14.sp)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    if (text.isEmpty()) return
    Box(
        modifier = Modifier
            .background(CalmColors.Surface, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun SituationCard(state: CoachUiState, modifier: Modifier = Modifier) {
    val s = state.snapshot
    Column(
        modifier = modifier
            .background(CalmColors.Surface, RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("지금 운전자는", color = CalmColors.TextDim, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                state.situation.headline,
                color = CalmColors.Accent,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 50.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(state.situation.detail, color = CalmColors.Text, fontSize = 18.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip("속도", s.speedKmh?.toString() ?: "–")
            Chip("기어", s.gear.toLabel())
            Chip("깜빡이", s.turnSignal.toLabel(), highlight = s.turnSignal != TurnSignal.OFF)
            Chip("브레이크", when (s.brakePressed) { true -> "밟음"; false -> "뗌"; null -> "–" }, highlight = s.brakePressed == true)
            Chip("핸들", s.steeringAngleDeg?.let { "${it.toInt()}°" } ?: "–")
        }

        if (state.lastSpoken != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CalmColors.SurfaceHigh, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("🔈 " + state.lastSpoken, color = CalmColors.Text, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Chip(label: String, value: String, highlight: Boolean = false) {
    Column(
        modifier = Modifier
            .background(if (highlight) CalmColors.Accent.copy(alpha = 0.18f) else CalmColors.SurfaceHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = CalmColors.TextDim, fontSize = 12.sp)
        Text(value, color = if (highlight) CalmColors.Accent else CalmColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CoachButtons(onCoach: (CoachPrompt) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("차가 대신 말해 줘요", color = CalmColors.TextDim, fontSize = 14.sp)
        CoachPrompt.entries.forEach { prompt ->
            Button(
                onClick = { onCoach(prompt) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prompt == CoachPrompt.DOING_GREAT) CalmColors.Good else CalmColors.SurfaceHigh,
                    contentColor = if (prompt == CoachPrompt.DOING_GREAT) CalmColors.Background else CalmColors.Text,
                )
            ) {
                Text(prompt.label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun BottomBar(state: CoachUiState, onOpenReport: () -> Unit, showDemoButton: Boolean, onToggleDemo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (state.canOpenReport) "정차 중이에요. 오늘 기록을 볼 수 있어요."
            else "주행 중에는 상황과 버튼만 보여요.",
            color = CalmColors.TextDim,
            fontSize = 14.sp
        )
        Spacer(Modifier.weight(1f))
        if (showDemoButton) {
            OutlinedButton(onClick = onToggleDemo, enabled = state.canOpenReport, shape = RoundedCornerShape(14.dp)) {
                Text(if (state.sourceLabel == "시연") "시연 끝" else "시연 주행", fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        OutlinedButton(onClick = onOpenReport, enabled = state.canOpenReport, shape = RoundedCornerShape(14.dp)) {
            Text("동승 리포트", fontSize = 16.sp)
        }
    }
}

private fun Gear.toLabel() = when (this) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
    Gear.UNKNOWN -> "–"
}

private fun TurnSignal.toLabel() = when (this) {
    TurnSignal.OFF -> "꺼짐"
    TurnSignal.LEFT -> "◀ 왼쪽"
    TurnSignal.RIGHT -> "오른쪽 ▶"
    TurnSignal.HAZARD -> "비상등"
}

@Suppress("unused")
private val previewSnapshot = VehicleSnapshot(speedKmh = 52, gear = Gear.DRIVE, turnSignal = TurnSignal.LEFT, brakePressed = false, steeringAngleDeg = -12f)
