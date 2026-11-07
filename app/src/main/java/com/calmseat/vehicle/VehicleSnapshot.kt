package com.calmseat.vehicle

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

enum class TurnSignal { OFF, LEFT, RIGHT, HAZARD }

// 차 상태 한 묶음. 못 읽은 값은 null
data class VehicleSnapshot(
    val speedKmh: Int? = null,
    val gear: Gear = Gear.UNKNOWN,
    val turnSignal: TurnSignal = TurnSignal.OFF,
    val brakePressed: Boolean? = null,
    val steeringAngleDeg: Float? = null, // 0이 중립
    val drivingRestricted: Boolean = false,
) {
    val isStopped: Boolean
        get() = gear == Gear.PARK || (speedKmh != null && speedKmh == 0)
}
