@file:Suppress("DEPRECATION")

package com.ble.kyv.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ble.kyv.ble.callbacks.*

@SuppressLint("MissingPermission")
class BLEManager private constructor(private val context: Context) : 
    GattCallbackListener, DataParser.ParseListener {
    
    companion object {
        @Volatile
        private var instance: BLEManager? = null
        
        fun getInstance(context: Context): BLEManager {
            return instance ?: synchronized(this) {
                instance ?: BLEManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val adapterBLE: BluetoothAdapter by lazy { 
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter 
    }
    private lateinit var scanner: BluetoothLeScanner
    private val logManager = BLELogManager(context)
    
    private var currentGatt = mutableListOf<Pair<BluetoothGatt?, String>>()
    private var currentDevice: Pair<DeviceType, String>? = null
    private var isScanning = false
    
    // Callbacks
    private var onConnectionChange: ((List<String>) -> Unit)? = null
    private var onMeasure: MeasureListener? = null
    private var onScanResult: ((List<ScanResult>) -> Unit)? = null
    
    // Parser and Gatt Callbacks
    private val parser = DataParser(this)
    private val bm1000cCallback = Bm1000cGattCallback(this)
    private val u807GattCallback = U807GattCallback(this)
    private val contourGattCallback = ContourGattCallback(this)
    private val ad805Callback = Ad805GattCallback(this)
    private val cf516Callback = Cf516GattCallback(this)
    private val ld575GattCallback = LD575GattCallback(this)
    
    private var isFinish = false
    
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            android.util.Log.d("BLEManager", "Found device: ${result.device.name ?: "Unknown"} (${result.device.address}) RSSI: ${result.rssi}")
            
            // Log scan event
            logManager.logEvent(
                result.device.address,
                BLEEvent.EventType.SCAN_FOUND,
                "Device found: ${result.device.name ?: "Unknown"}",
                "RSSI: ${result.rssi} dBm"
            )
            
            if (!adapterBLE.isEnabled) {
                stopScan()
                return
            }
            
            // Accept all devices, even without name
            onScanResult?.invoke(listOf(result))
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            android.util.Log.d("BLEManager", "Batch scan results: ${results.size} devices")
            onScanResult?.invoke(results)
        }
        
        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("BLEManager", "Scan failed with error: $errorCode")
            val errorMessage = when(errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error: $errorCode"
            }
            Toast.makeText(context, "Scan failed: $errorMessage", Toast.LENGTH_LONG).show()
            super.onScanFailed(errorCode)
        }
    }
    
    fun startScan(savedDevices: List<Device> = emptyList(), onResult: (List<ScanResult>) -> Unit) {
        if (isScanning) return
        
        onScanResult = onResult
        
        if (!adapterBLE.isEnabled) {
            Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        
        scanner = adapterBLE.bluetoothLeScanner
        
        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && adapterBLE.isLeCodedPhySupported) {
            settingsBuilder.setLegacy(false)
            settingsBuilder.setPhy(BluetoothDevice.PHY_LE_1M)
        }
        
        // Scan all BLE devices without filters to find everything
        val filters = if (savedDevices.isNotEmpty()) {
            savedDevices.map { device ->
                ScanFilter.Builder()
                    .setDeviceAddress(device.mac)
                    .build()
            }
        } else {
            // No filters - scan all BLE devices
            emptyList()
        }
        
        android.util.Log.d("BLEManager", "Starting BLE scan with ${filters.size} filters")
        scanner.startScan(filters, settingsBuilder.build(), scanCallback)
        isScanning = true
        Toast.makeText(context, "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()
    }
    
    fun stopScan() {
        if (!isScanning) return
        if (adapterBLE.isEnabled) {
            scanner.stopScan(scanCallback)
        }
        isScanning = false
        onScanResult = null
    }
    
    fun connect(device: Device) {
        val remote = adapterBLE.getRemoteDevice(device.mac) ?: return
        
        logManager.logEvent(
            device.mac,
            BLEEvent.EventType.CONNECTING,
            "Connecting to ${device.name}",
            "Type: ${device.type}"
        )
        
        currentGatt.add(Pair(null, device.mac))
        
        when (device.type) {
            DeviceType.BM1000C -> {
                remote.connectGatt(context, false, bm1000cCallback.setMac(device.mac), 
                    BluetoothDevice.TRANSPORT_LE)
                isFinish = false
            }
            DeviceType.AD805 -> {
                remote.connectGatt(context, false, ad805Callback.setMac(device.mac), 
                    BluetoothDevice.TRANSPORT_LE)
                isFinish = false
            }
            DeviceType.WEIGHT -> {
                remote.connectGatt(context, false, cf516Callback.setMac(device.mac), 
                    BluetoothDevice.TRANSPORT_LE)
                isFinish = false
            }
            DeviceType.U807 -> {
                remote.connectGatt(context, false, u807GattCallback.setMac(device.mac), 
                    BluetoothDevice.TRANSPORT_LE)
                isFinish = false
            }
            DeviceType.CONTOUR -> {
                remote.connectGatt(context, false, contourGattCallback.setMac(device.mac))
                isFinish = false
            }
            DeviceType.LD575 -> {
                remote.connectGatt(context, false, ld575GattCallback.setMac(device.mac), 
                    BluetoothDevice.TRANSPORT_LE)
                isFinish = false
            }
            else -> {}
        }
    }
    
    fun disconnect(mac: String) {
        val gatt = currentGatt.firstOrNull { it.second == mac }
        gatt?.first?.disconnect()
        currentGatt.removeAll { it.second == mac }
        onConnectionChange?.invoke(currentGatt.map { it.second })
    }
    
    fun disconnectAll() {
        currentGatt.forEach { it.first?.disconnect() }
        currentGatt.clear()
        currentDevice = null
        onConnectionChange?.invoke(emptyList())
    }
    
    fun setOnConnectionChangeListener(listener: (List<String>) -> Unit) {
        onConnectionChange = listener
        onConnectionChange?.invoke(currentGatt.map { it.second })
    }
    
    fun clearOnConnectionChangeListener() {
        onConnectionChange = null
    }
    
    fun setMeasureListener(listener: MeasureListener) {
        isFinish = false
        if (currentDevice?.first == DeviceType.BM1000C || currentDevice?.first == DeviceType.AD805)
            parser.clearBM1000CBuffer()
        onMeasure = listener
        if (currentDevice?.first == DeviceType.CONTOUR)
            parser.repeatContour(currentDevice?.second ?: "")
    }
    
    fun clearMeasureListener() {
        if (currentDevice?.first == DeviceType.BM1000C || currentDevice?.first == DeviceType.AD805)
            parser.clearBM1000CBuffer()
        onMeasure = null
    }
    
    fun getMeasureDeviceType(): DeviceType? {
        return currentDevice?.first
    }
    
    fun isMeasureFinish(): Boolean = isFinish
    
    fun getConnectedDevices(): List<String> {
        return currentGatt.map { it.second }
    }
    
    // GattCallbackListener implementation
    override fun onConnect(gatt: BluetoothGatt, mac: String) {
        logManager.logEvent(
            mac,
            BLEEvent.EventType.CONNECTED,
            "Successfully connected to device",
            "Services: ${gatt.services.size}"
        )
        
        currentGatt.removeAll { it.second == mac }
        currentGatt.add(Pair(gatt, mac))
        handler.post {
            onConnectionChange?.invoke(currentGatt.map { it.second })
        }
    }
    
    override fun onDisconnect(gatt: BluetoothGatt?, mac: String) {
        logManager.logEvent(
            mac,
            BLEEvent.EventType.DISCONNECTED,
            "Device disconnected",
            null
        )
        
        if (gatt != null) {
            currentGatt.removeAll { it.second == mac }
        }
        if (currentDevice != null && currentDevice?.second == mac) {
            currentDevice = null
            handler.post {
                onMeasure?.disconnect()
            }
        }
        handler.post {
            onConnectionChange?.invoke(currentGatt.map { it.second })
        }
    }
    
    override fun onError(error: String, gatt: BluetoothGatt, mac: String) {
        logManager.logEvent(
            mac,
            BLEEvent.EventType.ERROR,
            "Error occurred",
            error
        )
        
        gatt.disconnect()
        currentGatt.removeAll { it.second == mac }
        handler.post {
            onConnectionChange?.invoke(currentGatt.map { it.second })
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onData(type: DeviceType, array: ByteArray, gatt: BluetoothGatt, mac: String) {
        if (currentDevice != null && currentDevice?.first != type && currentDevice?.second != mac)
            return
        
        logManager.logEvent(
            mac,
            BLEEvent.EventType.DATA_RECEIVED,
            "Data received from ${type}",
            "Bytes: ${array.size}"
        )
        
        when(type) {
            DeviceType.BM1000C -> parser.readBM1000C(array, mac)
            DeviceType.AD805 -> parser.readAD805(array, mac)
            DeviceType.U807 -> parser.readU807(array, mac)
            DeviceType.CONTOUR -> parser.readContour(array, mac)
            DeviceType.WEIGHT -> parser.readCF516(array, mac)
            DeviceType.LD575 -> parser.readLD575(array, mac)
            DeviceType.UNKNOWN -> {}
        }
    }
    
    // DataParser.ParseListener implementation
    override fun onBM1000CRead(spo2: Int, pulseRate: Int, mac: String) {
        if (isFinish) return
        
        if (currentDevice == null) {
            currentDevice = Pair(DeviceType.BM1000C, mac)
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_START,
                "Measurement started (BM1000C)",
                null
            )
            handler.post {
                onMeasure?.onDeviceSelected()
            }
        } else {
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_PROGRESS,
                "SPO2: $spo2%, Pulse: $pulseRate bpm",
                null
            )
            handler.post {
                onMeasure?.measurePulseOximeter(spo2, pulseRate)
            }
        }
    }
    
    override fun onBM1000CFinish(spo2: Int, pulseRate: Int, mac: String) {
        pulseOximeterFinish(spo2, pulseRate, mac)
    }
    
    override fun onAD805Read(spo2: Int, pulseRate: Int, mac: String) {
        if (isFinish) return
        
        if (currentDevice == null) {
            currentDevice = Pair(DeviceType.AD805, mac)
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_START,
                "Measurement started (AD805)",
                null
            )
            handler.post {
                onMeasure?.onDeviceSelected()
            }
        } else {
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_PROGRESS,
                "SPO2: $spo2%, Pulse: $pulseRate bpm",
                null
            )
            handler.post {
                onMeasure?.measurePulseOximeter(spo2, pulseRate)
            }
        }
    }
    
    override fun onAD805Finish(spo2: Int, pulseRate: Int, mac: String) {
        pulseOximeterFinish(spo2, pulseRate, mac)
    }
    
    private fun pulseOximeterFinish(spo2: Int, pulseRate: Int, mac: String) {
        if (isFinish) return
        
        logManager.logEvent(
            mac,
            BLEEvent.EventType.MEASUREMENT_COMPLETE,
            "Pulse Oximeter measurement complete",
            "SPO2: $spo2%, Pulse: $pulseRate bpm"
        )
        
        handler.post {
            onMeasure?.finishPulseOximeter(spo2, pulseRate, mac)
        }
        
        if (!isFinish) {
            isFinish = true
        }
        
        currentDevice = null
        parser.clearBM1000CBuffer()
    }
    
    override fun onCF516Read(weight: Double, mac: String) {
        if (isFinish) return
        
        if (currentDevice == null) {
            currentDevice = Pair(DeviceType.WEIGHT, mac)
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_START,
                "Measurement started (Weight Scale)",
                null
            )
            handler.post {
                onMeasure?.onDeviceSelected()
            }
        } else {
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_PROGRESS,
                "Weight: ${String.format("%.1f", weight)} lb",
                null
            )
            handler.post {
                onMeasure?.measureWeight(weight)
            }
        }
    }
    
    override fun onCF516Finish(weight: Double, mac: String) {
        if (isFinish) return
        if (weight < 0.1) return
        
        logManager.logEvent(
            mac,
            BLEEvent.EventType.MEASUREMENT_COMPLETE,
            "Weight measurement complete",
            "Weight: ${String.format("%.1f", weight)} lb"
        )
        
        handler.post {
            onMeasure?.finishWeight(weight)
        }
        
        currentDevice = null
    }
    
    override fun onU807Read(pressureH: Int, pressureL: Int, mac: String) {
        if (currentDevice == null) {
            currentDevice = Pair(DeviceType.U807, mac)
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_START,
                "Measurement started (Blood Pressure)",
                null
            )
            handler.post {
                onMeasure?.onDeviceSelected()
            }
        } else {
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_PROGRESS,
                "BP: $pressureH/$pressureL mmHg",
                null
            )
            handler.post {
                onMeasure?.measureBloodPressure(pressureH, pressureL)
            }
        }
    }
    
    override fun onU807Finish(sys: Int, dia: Int, pul: Int, mac: String) {
        if (isFinish) return
        
        logManager.logEvent(
            mac,
            BLEEvent.EventType.MEASUREMENT_COMPLETE,
            "Blood pressure measurement complete",
            "SYS: $sys mmHg, DIA: $dia mmHg, PUL: $pul bpm"
        )
        
        handler.post {
            onMeasure?.finishBloodPressure(sys, dia, pul, mac)
        }
        
        if (!isFinish) {
            isFinish = true
        }
        
        currentDevice = null
    }
    
    override fun onGlucoseFinish(mgdl: Float, mac: String) {
        if (currentDevice == null) {
            currentDevice = Pair(DeviceType.CONTOUR, mac)
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_START,
                "Measurement started (Glucose Meter)",
                null
            )
            handler.post {
                onMeasure?.onDeviceSelected()
            }
        } else {
            if (isFinish) return
            
            logManager.logEvent(
                mac,
                BLEEvent.EventType.MEASUREMENT_COMPLETE,
                "Glucose measurement complete",
                "Glucose: ${mgdl.toInt()} mg/dL"
            )
            
            handler.post {
                onMeasure?.finishGlucose(mgdl, mac)
            }
            
            if (!isFinish) {
                isFinish = true
            }
            
            currentDevice = null
        }
    }
}

