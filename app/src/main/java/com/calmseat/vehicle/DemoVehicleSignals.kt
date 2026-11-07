package com.calmseat.vehicle

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 차 없을 때 돌리는 가짜 주행. 출발 -> 차선변경(깜빡이 안켬) -> 차선변경(깜빡이 켬) -> 정차 반복
class DemoVehicleSignals : VehicleSignals {

    override val sourceLabel: String = "데모"

    private data class Step(val seconds: Int, val snapshot: VehicleSnapshot)

    private val script: List<Step> = buildList {
        val parked = VehicleSnapshot(speedKmh = 0, gear = Gear.PARK, brakePressed = true, steeringAngleDeg = 0f)
        add(Step(4, parked))
        add(Step(2, parked.copy(gear = Gear.DRIVE)))
        for (v in listOf(8, 18, 30, 42, 50)) {
            add(Step(1, VehicleSnapshot(speedKmh = v, gear = Gear.DRIVE, brakePressed = false, steeringAngleDeg = 0f)))
        }
        // 깜빡이 안 켜고 오른쪽으로
        add(Step(1, cruising(52, steering = 40f)))
        add(Step(1, cruising(52, steering = 70f)))
        add(Step(1, cruising(52, steering = 20f)))
        add(Step(3, cruising(55)))
        // 이번엔 깜빡이 켜고 왼쪽으로
        add(Step(2, cruising(55, signal = TurnSignal.LEFT)))
        add(Step(1, cruising(55, signal = TurnSignal.LEFT, steering = -45f)))
        add(Step(1, cruising(55, signal = TurnSignal.LEFT, steering = -70f)))
        add(Step(1, cruising(55, signal = TurnSignal.LEFT, steering = -15f)))
        add(Step(3, cruising(58)))
        for (v in listOf(45, 30, 15, 5, 0)) {
            add(Step(1, VehicleSnapshot(speedKmh = v, gear = Gear.DRIVE, brakePressed = true, steeringAngleDeg = 0f)))
        }
        add(Step(3, VehicleSnapshot(speedKmh = 0, gear = Gear.DRIVE, brakePressed = true, steeringAngleDeg = 0f)))
        add(Step(5, parked))
    }

    private fun cruising(speed: Int, signal: TurnSignal = TurnSignal.OFF, steering: Float = 0f) =
        VehicleSnapshot(speedKmh = speed, gear = Gear.DRIVE, turnSignal = signal, brakePressed = false, steeringAngleDeg = steering)

    override fun snapshots(): Flow<VehicleSnapshot> = flow {
        while (true) {
            for (step in script) {
                emit(step.snapshot)
                delay(step.seconds * 1000L)
            }
        }
    }
}
