package com.ble.kyv.ble.callbacks

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.ble.kyv.ble.Const
import com.ble.kyv.ble.DeviceType

class U807GattCallback(listener: GattCallbackListener): DeviceGattCallback(listener) {
    private val startMeasure = byteArrayOf((0xFD).toByte(), (0xFD).toByte(), (0xFA).toByte(), (0x05).toByte(), (0X0D).toByte(), (0x0A).toByte())
    private val offDevice = byteArrayOf(0xFD.toByte(),0xFD.toByte(),0xFE.toByte(), 0x06.toByte(), 0X0D.toByte(), 0x0A.toByte())
    private var isOffing = false
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    fun setMac(mac: String): U807GattCallback {
        this.mac = mac
        return this
    }

    override fun initCharacteristic(gatt: BluetoothGatt) {
        val services = gatt.services
        val service = services.firstOrNull { service -> return@firstOrNull service?.uuid == Const.UUID_SERVICE_BP }
        writeCharacteristic = service?.characteristics?.firstOrNull { it.uuid == Const.UUID_BP_CHARACTER_WRITE }
        val characteristic = service?.characteristics?.firstOrNull { it.uuid == Const.UUID_BP_CHARACTER_RECEIVE }
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(Const.UUID_CLIENT_CHARACTER_CONFIG) ?: return
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            listener.onError("initCharacteristic PO characteristic not found", gatt, mac!!)
            println("initCharacteristic PO characteristic not found")
        }
    }

    override fun decodeBytes(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
        val value: ByteArray = characteristic.value
        when {
            (value.size == 1 && value[0] == (0xA5).toByte()) -> {
                println("BP write that ready")
                writeCharacteristic?.value = startMeasure
                gatt.writeCharacteristic(writeCharacteristic)
            }
            (value.size == 5) -> {
                println("Ready to measure") }
            (value.size == 6) -> {
                val reason = when(value[3]) {
                    0x0E.toByte() -> "The BP monitor is abnormal, pls contact the provider"
                    0x01.toByte() -> "The heart rate signal is too weak or the pressure fall suddenly"
                    0x02.toByte() -> "Other signal disturb"
                    0x03.toByte() -> "Inflation time too long"
                    0x05.toByte() -> "Test result abnormal"
                    0x0C.toByte() -> "Correction abnormal"
                    0x0D.toByte() -> "Low voltage (low power, pls change batteries)"
                    else -> ""

                }
                turnOffDevice(gatt)
                listener.onError("Measure error: $reason", gatt, mac!!)
            }
            (value.size == 8) -> {
                if (!isOffing) {
                    sendData(value, gatt)
//                    turnOffDevice(gatt)
//                    gatt.disconnect()
                }
            }
            else -> {
                sendData(value, gatt)
            }
        }
    }

    private fun turnOffDevice(gatt: BluetoothGatt) {
        isOffing = true
        for (i in 1..6) {
            writeCharacteristic?.value = offDevice
            gatt.writeCharacteristic(writeCharacteristic)
        }
    }

    override fun sendData(array: ByteArray, gatt: BluetoothGatt) {
        listener.onData(DeviceType.U807, array, gatt, mac!!)

    }
}