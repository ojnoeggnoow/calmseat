package com.calmseat.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlin.math.roundToInt

// 속도는 Pleos SDK에 없어서 AAOS 기본 API로 받음. 기어도 여기서 한번 더 받아둠
class AaosCarSignals(private val context: Context) {

    private val car: Car? by lazy {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return@lazy null
        runCatching { Car.createCar(context) }
            .onFailure { Log.w(TAG, "Car 서비스에 연결하지 못했습니다.", it) }
            .getOrNull()
    }

    val isAvailable: Boolean
        get() = car != null

    // m/s로 옴
    fun speedKmh(): Flow<Int?> = property(VehiclePropertyIds.PERF_VEHICLE_SPEED) { raw ->
        (raw as? Float)?.let { (it * 3.6f).roundToInt() }
    }

    fun gear(): Flow<Gear?> = property(VehiclePropertyIds.GEAR_SELECTION) { raw ->
        when (raw as? Int) {
            GEAR_PARK -> Gear.PARK
            GEAR_REVERSE -> Gear.REVERSE
            GEAR_NEUTRAL -> Gear.NEUTRAL
            GEAR_DRIVE -> Gear.DRIVE
            null -> null
            else -> Gear.UNKNOWN
        }
    }

    fun drivingRestricted(): Flow<Boolean> {
        val manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            ?: return flowOf(false)
        return callbackFlow {
            trySend(manager.currentCarUxRestrictions?.isRequiresDistractionOptimization ?: false)
            val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
                trySend(restrictions.isRequiresDistractionOptimization)
            }
            runCatching { manager.registerListener(listener) }
            awaitClose { runCatching { manager.unregisterListener() } }
        }.distinctUntilChanged()
    }

    // subscribePropertyEvents는 API 35부터라 그냥 옛날 콜백 씀
    @Suppress("DEPRECATION")
    private fun <T> property(propertyId: Int, convert: (Any?) -> T?): Flow<T?> {
        val manager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            ?: return flowOf(null)
        return callbackFlow {
            val callback = object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(value: CarPropertyValue<*>) {
                    trySend(convert(value.value))
                }

                override fun onErrorEvent(propertyId: Int, areaId: Int) {
                    trySend(null)
                }
            }
            val ok = runCatching {
                manager.registerCallback(callback, propertyId, CarPropertyManager.SENSOR_RATE_UI)
            }.getOrDefault(false)
            if (!ok) {
                Log.w(TAG, "속성 구독 실패: $propertyId")
                trySend(null)
            }
            awaitClose { runCatching { manager.unregisterCallback(callback) } }
        }.distinctUntilChanged()
    }

    private companion object {
        const val TAG = "AaosCarSignals"

        // android.car.VehicleGear 값
        const val GEAR_NEUTRAL = 0x0001
        const val GEAR_REVERSE = 0x0002
        const val GEAR_PARK = 0x0004
        const val GEAR_DRIVE = 0x0008
    }
}
