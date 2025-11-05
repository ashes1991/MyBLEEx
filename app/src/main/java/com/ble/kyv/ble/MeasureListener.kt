package com.ble.kyv.ble

interface MeasureListener {
    fun measureWeight(weight: Double)
    fun measurePulseOximeter(spo2: Int, pulseRate: Int)
    fun measureBloodPressure(pressureH: Int, pressureL: Int)
    fun finishPulseOximeter(spo2: Int, pulseRate: Int, mac: String)
    fun finishBloodPressure(sys: Int, dia: Int, pul: Int, mac: String)
    fun finishGlucose(mmol: Float, mac: String)
    fun finishWeight(weight: Double)
    fun onDeviceSelected()
    fun disconnect()
}