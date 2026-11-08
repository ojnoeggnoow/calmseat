package com.calmseat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calmseat.vehicle.DemoVehicleSignals
import com.calmseat.vehicle.Gear
import com.calmseat.vehicle.PleosVehicleSignals
import com.calmseat.vehicle.TurnSignal
import com.calmseat.vehicle.VehicleSignals
import com.calmseat.vehicle.VehicleSnapshot
import com.calmseat.voice.AndroidSpeaker
import com.calmseat.voice.PleosSpeaker
import com.calmseat.voice.Speaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

// 동승자 버튼. 누르면 차가 대신 말해줌
enum class CoachPrompt(val label: String, val spoken: String) {
    RIGHT_CLEAR("오른쪽 비었어", "오른쪽 뒤, 비어 있어요. 천천히 들어가도 돼요."),
    LEFT_CLEAR("왼쪽 비었어", "왼쪽 뒤, 비어 있어요. 천천히 들어가도 돼요."),
    SLOW_DOWN("천천히", "조금만 천천히 가요. 잘하고 있어요."),
    STOP_HERE("여기서 멈춰", "여기서 부드럽게 멈춰요."),
    DOING_GREAT("잘하고 있어", "잘하고 있어요. 이대로 가요."),
}

data class CoachEvent(
    val atMillis: Long,
    val prompt: CoachPrompt,
    val speedKmh: Int?,
)

// 핸들 크게 꺾은 거 한 번. 깜빡이 켰었는지 같이 저장
data class Maneuver(
    val atMillis: Long,
    val speedKmh: Int?,
    val signaled: Boolean,
)

data class Situation(
    val headline: String,
    val detail: String,
)

data class CoachUiState(
    val snapshot: VehicleSnapshot = VehicleSnapshot(),
    val situation: Situation = Situation("연결 중", "차량 신호를 기다리고 있어요"),
    val sourceLabel: String = "",
    val voiceLabel: String = "",
    val lastSpoken: String? = null,
    val events: List<CoachEvent> = emptyList(),
    val maneuvers: List<Maneuver> = emptyList(),
    val tripStartMillis: Long = System.currentTimeMillis(),
    val maxSpeedKmh: Int = 0,
) {
    val canOpenReport: Boolean
        get() = snapshot.isStopped
}

class CoachViewModel(app: Application) : AndroidViewModel(app) {

    private val realSignals: VehicleSignals? = PleosVehicleSignals.connectOrNull(app)
    private val demoSignals = DemoVehicleSignals()
    private var signals: VehicleSignals = realSignals ?: demoSignals
    private var collectJob: Job? = null

    private val speaker: Speaker = PleosSpeaker(app, AndroidSpeaker(app))

    private val _uiState = MutableStateFlow(
        CoachUiState(sourceLabel = signals.sourceLabel, voiceLabel = speaker.engineLabel)
    )
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    private var lastSignalOnMillis = 0L
    private var steeringArmed = true

    init {
        startCollecting()
    }

    private fun startCollecting() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            signals.snapshots().collect { snapshot -> onSnapshot(snapshot) }
        }
    }

    // 실차 연결 상태에서도 시연용 가짜 주행을 돌릴 수 있게. 정차 중에만 누르게 UI에서 막음
    fun toggleDemo() {
        val real: VehicleSignals = realSignals ?: return
        val next: VehicleSignals = if (signals === demoSignals) real else demoSignals
        signals = next
        val label = if (next === demoSignals) "시연" else next.sourceLabel
        _uiState.update { it.copy(sourceLabel = label) }
        startCollecting()
    }

    val canToggleDemo: Boolean get() = realSignals != null

    fun onCoach(prompt: CoachPrompt) {
        speaker.speak(prompt.spoken)
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            state.copy(
                lastSpoken = prompt.spoken,
                voiceLabel = speaker.engineLabel,
                events = state.events + CoachEvent(now, prompt, state.snapshot.speedKmh),
            )
        }
    }

    private fun onSnapshot(snapshot: VehicleSnapshot) {
        val now = System.currentTimeMillis()
        if (snapshot.turnSignal != TurnSignal.OFF) lastSignalOnMillis = now

        val newManeuver = detectManeuver(snapshot, now)

        _uiState.update { state ->
            state.copy(
                snapshot = snapshot,
                situation = describe(snapshot),
                voiceLabel = speaker.engineLabel,
                maneuvers = if (newManeuver != null) state.maneuvers + newManeuver else state.maneuvers,
                maxSpeedKmh = maxOf(state.maxSpeedKmh, snapshot.speedKmh ?: 0),
            )
        }
    }

    // 15km/h 넘는 상태에서 핸들 35도 넘어가면 차선변경/회전 한 번으로 침
    // 4초 안에 깜빡이 켜져 있었으면 signaled
    private fun detectManeuver(snapshot: VehicleSnapshot, now: Long): Maneuver? {
        val angle = snapshot.steeringAngleDeg ?: return null
        val speed = snapshot.speedKmh ?: return null
        val turning = abs(angle) >= MANEUVER_ANGLE_DEG && speed >= MANEUVER_MIN_SPEED_KMH
        if (!turning) {
            if (abs(angle) < MANEUVER_RESET_DEG) steeringArmed = true
            return null
        }
        if (!steeringArmed) return null
        steeringArmed = false
        val signaled = snapshot.turnSignal != TurnSignal.OFF || now - lastSignalOnMillis <= SIGNAL_GRACE_MILLIS
        return Maneuver(now, speed, signaled)
    }

    // 동승자한테 보여줄 한 줄. 위에서부터 먼저 걸리는 걸로
    private fun describe(s: VehicleSnapshot): Situation {
        val speedText = s.speedKmh?.let { "${it}km/h" } ?: "속도 모름"
        return when {
            s.gear == Gear.PARK ->
                Situation("정차 중", "P단 · 안전하게 서 있어요")
            s.gear == Gear.REVERSE ->
                Situation("후진 중", "뒤를 함께 봐 주세요")
            s.turnSignal == TurnSignal.HAZARD ->
                Situation("비상등", "잠시 멈추거나 주의를 알리는 중")
            s.turnSignal == TurnSignal.LEFT ->
                Situation("왼쪽으로 갈 준비", "깜빡이 켬 · 왼쪽 뒤를 봐 주면 좋아요 · $speedText")
            s.turnSignal == TurnSignal.RIGHT ->
                Situation("오른쪽으로 갈 준비", "깜빡이 켬 · 오른쪽 뒤를 봐 주면 좋아요 · $speedText")
            s.brakePressed == true && (s.speedKmh ?: 1) > 0 ->
                Situation("속도 줄이는 중", "브레이크 밟음 · $speedText")
            s.brakePressed == true ->
                Situation("멈춰 있음", "브레이크 밟음 · 출발 대기")
            (s.steeringAngleDeg?.let { abs(it) } ?: 0f) >= MANEUVER_ANGLE_DEG ->
                Situation("핸들 돌리는 중", "차선 변경 또는 회전 · $speedText")
            s.speedKmh != null && s.speedKmh > 0 ->
                Situation("직진 중", "$speedText · 평소처럼 가고 있어요")
            else ->
                Situation("대기 중", "차량 신호를 기다리고 있어요")
        }
    }

    override fun onCleared() {
        realSignals?.close()
        speaker.shutdown()
    }

    private companion object {
        const val MANEUVER_ANGLE_DEG = 35f
        const val MANEUVER_RESET_DEG = 15f
        const val MANEUVER_MIN_SPEED_KMH = 15
        const val SIGNAL_GRACE_MILLIS = 4_000L
    }
}
