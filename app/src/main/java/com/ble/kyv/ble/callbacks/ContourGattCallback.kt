package com.ble.kyv.ble.callbacks

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.ble.kyv.ble.Const
import com.ble.kyv.ble.DeviceType

class ContourGattCallback(listener: GattCallbackListener): DeviceGattCallback(listener) {

    fun setMac(mac: String): ContourGattCallback {
        this.mac = mac
        return this
    }

    override fun initCharacteristic(gatt: BluetoothGatt) {
        val services = gatt.services
        val service = services.firstOrNull { service -> return@firstOrNull service?.uuid == Const.UUID_SERVICE_GLUCOSE }
        val characteristic = service?.characteristics?.firstOrNull { it.uuid == Const.UUID_GLUCOSE_CHARACTER_RECEIVE }
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(Const.UUID_CLIENT_CHARACTER_CONFIG)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            listener.onError("initCharacteristic PO characteristic not found", gatt, mac ?: "")
            println("initCharacteristic PO characteristic not found")
        }
    }

    override fun decodeBytes(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
        if (characteristic.value.size >= 14) {
            sendData(characteristic.value, gatt)
        }
    }

    override fun sendData(array: ByteArray, gatt: BluetoothGatt) {
        listener.onData(DeviceType.CONTOUR, array, gatt, mac ?: "")
    }
}