package com.calmseat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calmseat.CoachUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 정차 중에만 보는 리포트
@Composable
fun ReportScreen(state: CoachUiState, onBack: () -> Unit) {
    val minutes = (state.drivingMillis / 60_000L).toInt()
    val maneuvers = state.maneuvers
    val signaledRate = if (maneuvers.isEmpty()) null else maneuvers.count { it.signaled } * 100 / maneuvers.size
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalmColors.Background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("오늘의 동승 리포트", color = CalmColors.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(14.dp)) { Text("돌아가기", fontSize = 16.sp) }
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("주행 시간", "${minutes}분", Modifier.weight(1f))
            Stat("최고 속도", "${state.maxSpeedKmh}km/h", Modifier.weight(1f))
            Stat("깜빡이 사용률", signaledRate?.let { "$it%" } ?: "–", Modifier.weight(1f),
                sub = if (maneuvers.isEmpty()) "차선 변경 없음" else "${maneuvers.count { it.signaled }}/${maneuvers.size}회")
            Stat("동승자 도움", "${state.events.size}회", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        // 왼쪽: 동승자가 누른 버튼. 오른쪽: 깜빡이 안 켜고 꺾은 순간 (주행 중엔 말 안 하고 여기서만 보여줌)
        val missed = maneuvers.filter { !it.signaled }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            Column(Modifier.weight(1f)) {
                Text("동승자가 도와준 순간", color = CalmColors.TextDim, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                if (state.events.isEmpty()) {
                    Text("아직 버튼을 누른 적이 없어요. 초보가 혼자서도 잘 달렸다는 뜻이에요.", color = CalmColors.Text, fontSize = 16.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.events.asReversed()) { event ->
                            ReportRow(
                                time = timeFormat.format(Date(event.atMillis)),
                                title = event.prompt.label,
                                titleColor = CalmColors.Accent,
                                trailing = event.speedKmh?.let { "${it}km/h" } ?: "",
                            )
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text("깜빡이 없이 꺾은 순간", color = CalmColors.TextDim, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                if (maneuvers.isEmpty()) {
                    Text("차선 변경이나 회전이 없었어요.", color = CalmColors.Text, fontSize = 16.sp)
                } else if (missed.isEmpty()) {
                    Text("전부 깜빡이를 켰어요. 오늘은 칭찬만 하면 돼요.", color = CalmColors.Text, fontSize = 16.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(missed.asReversed()) { m ->
                            ReportRow(
                                time = timeFormat.format(Date(m.atMillis)),
                                title = "${m.direction}으로 꺾음 · 깜빡이 없음",
                                titleColor = CalmColors.Warn,
                                trailing = m.speedKmh?.let { "${it}km/h" } ?: "",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(time: String, title: String, titleColor: Color, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalmColors.Surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(time, color = CalmColors.TextDim, fontSize = 14.sp)
        Spacer(Modifier.width(16.dp))
        Text(title, color = titleColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(trailing, color = CalmColors.TextDim, fontSize = 14.sp)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    Column(
        modifier = modifier
            .background(CalmColors.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(label, color = CalmColors.TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = CalmColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = CalmColors.TextDim, fontSize = 12.sp)
    }
}
