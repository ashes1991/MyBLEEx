package com.ble.kyv.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile

abstract class DeviceGattCallback(protected val listener: GattCallbackListener): BluetoothGattCallback() {
    var mac: String? = null
    var thisGatt: BluetoothGatt? = null
    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            thisGatt = gatt
            listener.onConnect(gatt!!, mac!!)
            println("Attempting to start service discovery:" + gatt.discoverServices())
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt?.close()
            listener.onDisconnect(thisGatt, mac!!)
            println("Disconnected from GATT server.")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            initCharacteristic(gatt)
        } else {
            listener.onError("onServicesDiscovered received: $status", gatt, mac!!)
            println("onServicesDiscovered received: $status")
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        println("onCharacteristicRead = $status")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            decodeBytes(characteristic, gatt)
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        println("onCharacteristicChanged")
        decodeBytes(characteristic, gatt)
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        println("onCharacteristicChanged new")
        decodeBytes(characteristic, gatt)
    }

    protected abstract fun initCharacteristic(gatt: BluetoothGatt)

    protected abstract fun decodeBytes(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt)

    protected abstract fun sendData(array: ByteArray, gatt: BluetoothGatt)
}