package com.ble.kyv.ble.callbacks

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.ble.kyv.ble.Const
import com.ble.kyv.ble.DeviceType
import java.util.*

class Ad805GattCallback(listener: GattCallbackListener): DeviceGattCallback(listener) {
    private var lastSend = Date().time / 1000
    fun setMac(mac: String): Ad805GattCallback {
        this.mac = mac
        return this
    }

    override fun initCharacteristic(gatt: BluetoothGatt) {
        val services = gatt.services
        val characteristic = services.firstOrNull { service ->
            return@firstOrNull if (service?.uuid == Const.UUID_SERVICE_PO_AD805) {
                val characteristic = service?.characteristics?.firstOrNull { characteristic ->
                    characteristic.uuid == Const.UUID_PO_AD805_CHARACTER_RECEIVE
                }
                characteristic != null
            } else {
                false
            }
        }?.characteristics?.firstOrNull { char ->
            char.uuid == Const.UUID_PO_AD805_CHARACTER_RECEIVE
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
        if (!checkTime()) return
        sendData(characteristic.value.toList().subList(0, 5).toTypedArray().toByteArray(), gatt)
    }

    private fun checkTime(): Boolean {
        return ((Date().time / 1000) - lastSend) > 0
    }

    override fun sendData(array: ByteArray, gatt: BluetoothGatt) {
        lastSend = Date().time / 1000
        listener.onData(DeviceType.AD805, array, gatt, mac!!)
    }
}