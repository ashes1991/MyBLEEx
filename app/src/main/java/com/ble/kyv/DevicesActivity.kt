package com.ble.kyv

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ble.kyv.ble.BLEManager
import com.ble.kyv.ble.Device
import com.ble.kyv.ble.DeviceType
import com.ble.kyv.ble.PreferencesUtils
import com.ble.kyv.databinding.ActivityDevicesBinding

class DevicesActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDevicesBinding
    private lateinit var bleManager: BLEManager
    private lateinit var preferencesUtils: PreferencesUtils
    private lateinit var devicesAdapter: DevicesAdapter
    private var scanDialog: AlertDialog? = null
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions required for BLE", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bleManager = BLEManager.getInstance(this)
        preferencesUtils = PreferencesUtils(this)
        
        setupRecyclerView()
        setupViews()
        checkAndRequestPermissions()
        loadDevices()
    }
    
    private fun setupRecyclerView() {
        devicesAdapter = DevicesAdapter(
            onDeviceClick = { device ->
                connectAndMeasure(device)
            },
            onDeleteClick = { device ->
                showDeleteConfirmation(device)
            },
            onDeviceLongClick = { device ->
                openDeviceLogs(device)
            }
        )
        
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DevicesActivity)
            adapter = devicesAdapter
        }
    }
    
    private fun setupViews() {
        binding.scanButton.setOnClickListener {
            if (hasRequiredPermissions()) {
                showScanDialog()
            } else {
                checkAndRequestPermissions()
            }
        }
        
        binding.addButton.setOnClickListener {
            showAddDeviceDialog()
        }
    }
    
    private fun loadDevices() {
        val devices = preferencesUtils.getDevices()
        devicesAdapter.submitList(devices)
        
        binding.emptyStateLayout.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
        binding.devicesRecyclerView.visibility = if (devices.isEmpty()) View.GONE else View.VISIBLE
    }
    
    private fun connectAndMeasure(device: Device) {
        if (!hasRequiredPermissions()) {
            checkAndRequestPermissions()
            return
        }
        
        // Connect to device and start measurement activity
        val intent = Intent(this, MeasureActivity::class.java)
        intent.putExtra("device_name", device.name)
        intent.putExtra("device_mac", device.mac)
        intent.putExtra("device_type", device.type.name)
        startActivity(intent)
    }
    
    private fun openDeviceLogs(device: Device) {
        val intent = Intent(this, DeviceLogsActivity::class.java)
        intent.putExtra("device_name", device.name)
        intent.putExtra("device_mac", device.mac)
        startActivity(intent)
    }
    
    private fun showScanDialog() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "Please grant all permissions first", Toast.LENGTH_LONG).show()
            checkAndRequestPermissions()
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_scan_devices, null)
        val scannedDevicesRecyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.scannedDevicesRecyclerView
        )
        val stopScanButton = dialogView.findViewById<android.widget.Button>(R.id.stopScanButton)
        val closeButton = dialogView.findViewById<android.widget.Button>(R.id.closeButton)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.scanProgressBar)
        val statusText = dialogView.findViewById<android.widget.TextView>(R.id.scanStatusText)
        
        val scannedDevicesAdapter = ScannedDevicesAdapter { scanResult ->
            // Add scanned device
            val deviceType = detectDeviceType(scanResult)
            val device = Device(
                name = scanResult.device.name ?: "Unknown Device",
                mac = scanResult.device.address,
                type = deviceType
            )
            preferencesUtils.addDevice(device)
            loadDevices()
            scanDialog?.dismiss()
            bleManager.stopScan()
        }
        
        scannedDevicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DevicesActivity)
            adapter = scannedDevicesAdapter
        }
        
        scanDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        stopScanButton.setOnClickListener {
            bleManager.stopScan()
            progressBar.visibility = View.GONE
            stopScanButton.isEnabled = false
            statusText.text = "Scan stopped"
        }
        
        closeButton.setOnClickListener {
            bleManager.stopScan()
            scanDialog?.dismiss()
        }
        
        scanDialog?.show()
        
        android.util.Log.d("DevicesActivity", "Starting BLE scan...")
        
        // Start scanning
        val scannedDevices = mutableListOf<ScanResult>()
        bleManager.startScan { results ->
            android.util.Log.d("DevicesActivity", "Received ${results.size} scan results")
            runOnUiThread {
                results.forEach { result ->
                    if (scannedDevices.none { it.device.address == result.device.address }) {
                        scannedDevices.add(result)
                        android.util.Log.d("DevicesActivity", "Added device: ${result.device.name ?: "Unknown"}")
                    }
                }
                scannedDevicesAdapter.submitList(scannedDevices.toList())
                statusText.text = "Found ${scannedDevices.size} device(s)"
            }
        }
    }
    
    private fun showAddDeviceDialog() {
        AddDeviceDialog(this) { device ->
            preferencesUtils.addDevice(device)
            loadDevices()
        }.show()
    }
    
    private fun showDeleteConfirmation(device: Device) {
        AlertDialog.Builder(this)
            .setTitle("Delete Device")
            .setMessage("Are you sure you want to delete ${device.name}?")
            .setPositiveButton("Delete") { _, _ ->
                preferencesUtils.removeDevice(device.mac)
                loadDevices()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun detectDeviceType(scanResult: ScanResult): DeviceType {
        val deviceName = scanResult.device.name ?: return DeviceType.UNKNOWN
        
        return when {
            deviceName.contains("BM1000", ignoreCase = true) -> DeviceType.BM1000C
            deviceName.contains("AD805", ignoreCase = true) -> DeviceType.AD805
            deviceName.contains("U807", ignoreCase = true) || deviceName.contains("BP", ignoreCase = true) -> DeviceType.U807
            deviceName.contains("Contour", ignoreCase = true) -> DeviceType.CONTOUR
            deviceName.contains("CF516", ignoreCase = true) || deviceName.contains("Weight", ignoreCase = true) -> DeviceType.WEIGHT
            deviceName.contains("LD575", ignoreCase = true) -> DeviceType.LD575
            else -> DeviceType.UNKNOWN
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bleManager.stopScan()
        scanDialog?.dismiss()
    }
}

