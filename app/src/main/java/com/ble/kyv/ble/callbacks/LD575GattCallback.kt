package com.ble.kyv.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.os.Handler
import android.os.Looper
import com.ble.kyv.ble.Const
import com.ble.kyv.ble.DeviceType
import java.util.Arrays

class LD575GattCallback(listener: GattCallbackListener): DeviceGattCallback(listener) {
    private var array = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 1, 1, 0xFE.toByte())
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    fun setMac(mac: String): LD575GattCallback {
        this.mac = mac
        return this
    }

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        println("onConnectionStateChange = $newState old = $status")
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            thisGatt = gatt
            listener.onConnect(gatt!!, mac!!)
            println("Attempting to start service discovery:" + gatt.discoverServices())
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt?.disconnect()
            listener.onDisconnect(gatt, mac!!)
            println("Disconnected from GATT server.")
        }
    }

    @SuppressLint("MissingPermission")
    override fun initCharacteristic(gatt: BluetoothGatt) {
        val services = gatt.services
        val service = services.firstOrNull { service -> return@firstOrNull service?.uuid == Const.UUID_SERVICE_BP_LD_575 }
        writeCharacteristic = service?.characteristics?.firstOrNull { it.uuid == Const.UUID_BP_LD_575_CHARACTER_WRITE }
        val characteristic = service?.characteristics?.firstOrNull { it.uuid == Const.UUID_BP_LD_575_CHARACTER_RECEIVE }
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true)
        } else {
            listener.onError("initCharacteristic PO characteristic not found", gatt, mac!!)
            println("initCharacteristic PO characteristic not found")
        }
    }

    override fun decodeBytes(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
        val value: ByteArray = characteristic.value ?: return
        println(Arrays.toString(value))
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        if (value.size > 5) {
            when {
                (value[3] == (0x0a).toByte()) -> {
                    sendData(value, gatt)
                }
                (value[3] == (0x49).toByte()) -> {
                    sendData(value, gatt)
                }
                (value[3] == (0x06).toByte()) -> {
                    val reason = when(value[5]) {
                        0x00.toByte() -> "The device is not inflating air."
                        0x01.toByte() -> "Error during measurement, please measure correctly."
                        0x02.toByte() -> "Low blood pressure monitor battery."
                        else -> "Unknown error"

                    }
                    listener.onError("Measure error: $reason", gatt, mac!!)
                }
            }
        }
        timeoutRunnable = Runnable {
            listener.onError("Measure error: Long time no data", gatt, mac!!)
        }
        handler.postDelayed(timeoutRunnable!!, 2500)
    }

    override fun sendData(array: ByteArray, gatt: BluetoothGatt) {
        listener.onData(DeviceType.LD575, array, gatt, mac!!)
    }

    fun createLbBp575Packet(commands: ByteArray, payload: ByteArray? = null): ByteArray {
        val header: Byte = 0xFF.toByte()
        val headerBytes = byteArrayOf(header, header)

        val n: Byte = ((payload?.size ?: 0) + commands.size).toByte()
        val packet = mutableListOf<Byte>()

        packet.addAll(headerBytes.toList())
        packet.add(n)
        packet.addAll(commands.toList())

        payload?.let {
            packet.addAll(it.toList())
        }

        // Compute sum: N + sum of commands + sum of payload (if any)
        var sum: Int = (n.toInt() and 0xFF) +
                commands.sumOf { it.toInt() and 0xFF } +
                (payload?.sumOf { it.toInt() and 0xFF } ?: 0)

        // Checksum = 2's complement (i.e. (-sum) & 0xFF)
        val checksum: Byte = ((256 - (sum % 256)) % 256).toByte()

        packet.add(checksum)

        return packet.toByteArray()
    }
}