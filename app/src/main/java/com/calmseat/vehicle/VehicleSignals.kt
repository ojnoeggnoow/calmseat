package com.calmseat.vehicle

import kotlinx.coroutines.flow.Flow

// 실차든 데모든 이걸로 받음
interface VehicleSignals {
    val sourceLabel: String

    fun snapshots(): Flow<VehicleSnapshot>

    fun close() {}
}
