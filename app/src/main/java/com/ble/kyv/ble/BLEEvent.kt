package com.ble.kyv.ble

import java.util.*

data class BLEEvent(
    val deviceMac: String,
    val eventType: EventType,
    val message: String,
    val timestamp: Long = Date().time,
    val details: String? = null
) {
    enum class EventType {
        SCAN_FOUND,
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR,
        DATA_RECEIVED,
        MEASUREMENT_START,
        MEASUREMENT_PROGRESS,
        MEASUREMENT_COMPLETE,
        SERVICE_DISCOVERED,
        CHARACTERISTIC_READ,
        CHARACTERISTIC_WRITE
    }
    
    fun getFormattedTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getShortTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

