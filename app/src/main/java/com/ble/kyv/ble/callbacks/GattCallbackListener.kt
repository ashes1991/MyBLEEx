package com.ble.kyv.ble.callbacks

import android.bluetooth.BluetoothGatt
import com.ble.kyv.ble.DeviceType

interface GattCallbackListener {
    fun onConnect(gatt: BluetoothGatt, mac: String)
    fun onDisconnect(gatt: BluetoothGatt?, mac: String)
    fun onError(error: String, gatt: BluetoothGatt, mac: String)
    fun onData(type: DeviceType, array: ByteArray, gatt: BluetoothGatt, mac: String)
}