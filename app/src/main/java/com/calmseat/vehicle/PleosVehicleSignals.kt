package com.calmseat.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

// 실차 신호. 깜빡이/브레이크/핸들/기어는 Pleos SDK, 속도는 AaosCarSignals
class PleosVehicleSignals private constructor(
    private val vehicle: Vehicle,
    private val aaos: AaosCarSignals,
) : VehicleSignals {

    override val sourceLabel: String = "차량 연결"

    override fun snapshots(): Flow<VehicleSnapshot> = combine(
        turnSignal(),
        brakePressed(),
        aaos.speedKmh(),
        gear(),
        steeringAngleDeg(),
        aaos.drivingRestricted(),
        aaos.gear(),
    ) { values ->
        VehicleSnapshot(
            turnSignal = values[0] as TurnSignal,
            brakePressed = values[1] as Boolean?,
            speedKmh = values[2] as Int?,
            gear = (values[3] as Gear?) ?: (values[6] as Gear?) ?: Gear.UNKNOWN,
            steeringAngleDeg = values[4] as Float?,
            drivingRestricted = values[5] as Boolean,
        )
    }

    private fun gear(): Flow<Gear?> = callbackFlow {
        val api = vehicle.getGear()
        val listener = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) {
                trySend(gear.toGear())
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "기어 신호 실패(권한 없음일 수 있음) → AAOS 값 사용", e)
                trySend(null)
            }
        }
        api.registerGearSelection(listener)
        awaitClose { runCatching { api.unregisterGearSelection(listener) } }
    }.catch { emit(null) }.distinctUntilChanged()

    private fun VehicleGear?.toGear(): Gear? = when (this) {
        null, VehicleGear.GEAR_UNKNOWN -> null
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        else -> Gear.DRIVE // 1~9단도 그냥 D로
    }

    private fun turnSignal(): Flow<TurnSignal> = callbackFlow {
        val api = vehicle.getTurnSignal()
        val listener = object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
                trySend(state.toTurnSignal())
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "방향지시등 신호 실패", e)
                trySend(TurnSignal.OFF)
            }
        }
        api.registerTurnSignalState(listener)
        awaitClose { runCatching { api.unregisterTurnSignalState(listener) } }
    }.catch { emit(TurnSignal.OFF) }.distinctUntilChanged()

    private fun brakePressed(): Flow<Boolean?> = callbackFlow {
        val api = vehicle.getBrake()
        val listener = object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(status: Boolean?) {
                trySend(status)
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "브레이크 신호 실패", e)
                trySend(null)
            }
        }
        api.registerBrakePedalStatus(listener)
        awaitClose { runCatching { api.unregisterBrakePedalStatus(listener) } }
    }.catch { emit(null) }.distinctUntilChanged()

    private fun steeringAngleDeg(): Flow<Float?> = callbackFlow {
        val api = vehicle.getSteeringWheel()
        val listener = object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(angle: Float?) {
                trySend(angle)
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "조향각 신호 실패", e)
                trySend(null)
            }
        }
        api.registerSteeringWheelAngle(listener)
        awaitClose { runCatching { api.unregisterSteeringWheelAngle(listener) } }
    }.catch { emit(null) }.distinctUntilChanged()

    override fun close() {
        runCatching { vehicle.release() }
    }

    // enum이 OFF/LEFT/RIGHT뿐인데 혹시 몰라서 이름으로 매칭
    private fun TurnSignalValue?.toTurnSignal(): TurnSignal {
        val name = this?.name?.uppercase() ?: return TurnSignal.OFF
        return when {
            "HAZARD" in name || "BOTH" in name -> TurnSignal.HAZARD
            "LEFT" in name -> TurnSignal.LEFT
            "RIGHT" in name -> TurnSignal.RIGHT
            else -> TurnSignal.OFF
        }
    }

    companion object {
        private const val TAG = "PleosVehicleSignals"

        // 연결 안 되면 null -> 데모 모드로 감
        fun connectOrNull(context: Context): PleosVehicleSignals? {
            val vehicle = runCatching { Vehicle(context.applicationContext).also { it.initialize() } }
                .onFailure { Log.w(TAG, "Pleos Vehicle SDK 연결 실패 → 데모 모드", it) }
                .getOrNull() ?: return null
            return PleosVehicleSignals(vehicle, AaosCarSignals(context.applicationContext))
        }
    }
}
