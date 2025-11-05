package com.ble.kyv.ble

import java.util.*

data class Device(
    val name: String,
    val mac: String,
    val type: DeviceType
) {
    var firstData: String? = null
    var secondData: String? = null
    var date: Date? = null

    var isDate = false

    fun getDayInMillis(calendar: Calendar): Long {
        date ?: return 0
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time.time
    }
}
