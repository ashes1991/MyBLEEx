package com.ble.kyv.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.ble.kyv.ble.Const
import com.ble.kyv.ble.DeviceType

@SuppressLint("MissingPermission")
class Cf516GattCallback(listener: GattCallbackListener): DeviceGattCallback(listener) {
//    private var lastSend = Date().time / 1000
    private var isFinish = false
    fun setMac(mac: String): Cf516GattCallback {
        this.mac = mac
        return this
    }

    override fun initCharacteristic(gatt: BluetoothGatt) {
        val services = gatt.services
        val characteristic = services.firstOrNull { service ->
            return@firstOrNull if (service?.uuid == Const.UUID_SERVICE_BP) {
                val characteristic = service?.characteristics?.firstOrNull { characteristic ->
                    characteristic.uuid == Const.UUID_WEIGHT_CHARACTER_RECEIVE
                }
                characteristic != null
            } else {
                false
            }
        }?.characteristics?.firstOrNull { char ->
            char.uuid == Const.UUID_WEIGHT_CHARACTER_RECEIVE
        }
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(Const.UUID_CLIENT_CHARACTER_CONFIG)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            listener.onError("initCharacteristic PO characteristic not found", gatt, mac!!)
            println("initCharacteristic PO characteristic not found")
        }
    }

    override fun decodeBytes(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
        val array = characteristic.value
        if (array.size > 2) {
            sendData(array, gatt)
        } else {
            listener.onError("Weight measure error!", gatt, mac!!)
        }

    }

    override fun sendData(array: ByteArray, gatt: BluetoothGatt) {
        if (!isFinish) {
            listener.onData(DeviceType.WEIGHT, array, gatt, mac!!) }
        else {
            isFinish = false
            return
        }
        if (array[9] == 0.toByte())
            isFinish = true
    }
}