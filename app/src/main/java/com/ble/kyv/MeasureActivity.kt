package com.ble.kyv

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ble.kyv.ble.BLEManager
import com.ble.kyv.ble.Device
import com.ble.kyv.ble.DeviceType
import com.ble.kyv.ble.MeasureListener
import com.ble.kyv.ble.PreferencesUtils
import com.ble.kyv.databinding.ActivityMeasureBinding

class MeasureActivity : AppCompatActivity(), MeasureListener {
    
    private lateinit var binding: ActivityMeasureBinding
    private lateinit var bleManager: BLEManager
    private lateinit var preferencesUtils: PreferencesUtils
    private var deviceMac: String = ""
    private var deviceType: DeviceType = DeviceType.UNKNOWN
    private var isFinished = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bleManager = BLEManager.getInstance(this)
        preferencesUtils = PreferencesUtils(this)
        
        // Get device info from intent
        val deviceName = intent.getStringExtra("device_name") ?: "Unknown"
        deviceMac = intent.getStringExtra("device_mac") ?: ""
        val deviceTypeString = intent.getStringExtra("device_type") ?: "UNKNOWN"
        deviceType = try {
            DeviceType.valueOf(deviceTypeString)
        } catch (e: Exception) {
            DeviceType.UNKNOWN
        }
        
        setupViews()
        connectToDevice()
    }
    
    private fun setupViews() {
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.completeButton.setOnClickListener {
            finish()
        }
        
        setDeviceTypeUI()
    }
    
    private fun setDeviceTypeUI() {
        when (deviceType) {
            DeviceType.BM1000C, DeviceType.AD805 -> {
                binding.statusText.text = "Place finger on sensor"
                binding.thirdDataLayout.visibility = View.GONE
            }
            DeviceType.U807, DeviceType.LD575 -> {
                binding.statusText.text = "Starting blood pressure measurement"
                binding.secondDataLayout.visibility = View.VISIBLE
                binding.thirdDataLayout.visibility = View.VISIBLE
            }
            DeviceType.CONTOUR -> {
                binding.statusText.text = "Insert test strip"
                binding.secondDataLayout.visibility = View.GONE
                binding.thirdDataLayout.visibility = View.GONE
            }
            DeviceType.WEIGHT -> {
                binding.statusText.text = "Step on the scale"
                binding.secondDataLayout.visibility = View.GONE
                binding.thirdDataLayout.visibility = View.GONE
            }
            else -> {
                binding.statusText.text = "Connecting..."
            }
        }
    }
    
    private fun connectToDevice() {
        val device = Device(
            name = intent.getStringExtra("device_name") ?: "Unknown",
            mac = deviceMac,
            type = deviceType
        )
        
        bleManager.setMeasureListener(this)
        bleManager.connect(device)
        
        binding.progressBar.visibility = View.VISIBLE
        binding.resultsContainer.visibility = View.GONE
    }
    
    // MeasureListener implementation
    override fun measureWeight(weight: Double) {
        if (isFinished) return
        runOnUiThread {
            binding.dataFirstText.text = String.format("%.1f", weight)
            binding.titleFirstText.text = "lb"
            binding.resultsContainer.visibility = View.VISIBLE
        }
    }
    
    override fun measurePulseOximeter(spo2: Int, pulseRate: Int) {
        if (isFinished) return
        runOnUiThread {
            binding.dataFirstText.text = spo2.toString()
            binding.titleFirstText.text = "%"
            
            binding.dataSecondText.text = pulseRate.toString()
            binding.titleSecondText.text = "BPM"
            binding.secondDataLayout.visibility = View.VISIBLE
            
            binding.resultsContainer.visibility = View.VISIBLE
        }
    }
    
    override fun measureBloodPressure(pressureH: Int, pressureL: Int) {
        if (isFinished) return
        runOnUiThread {
            binding.dataFirstText.text = pressureL.toString()
            binding.titleFirstText.text = "mmHg"
            binding.resultsContainer.visibility = View.VISIBLE
        }
    }
    
    override fun finishPulseOximeter(spo2: Int, pulseRate: Int, mac: String) {
        if (isFinished) return
        isFinished = true
        
        runOnUiThread {
            binding.statusText.text = "Measurement Complete"
            binding.dataFirstText.text = spo2.toString()
            binding.titleFirstText.text = "%"
            
            binding.dataSecondText.text = pulseRate.toString()
            binding.titleSecondText.text = "BPM"
            binding.secondDataLayout.visibility = View.VISIBLE
            
            binding.progressBar.visibility = View.GONE
            binding.resultsContainer.visibility = View.VISIBLE
            binding.completeButton.visibility = View.VISIBLE
            
            // Save measurement
            preferencesUtils.updateDeviceData(mac, spo2.toString(), pulseRate.toString())
        }
    }
    
    override fun finishBloodPressure(sys: Int, dia: Int, pul: Int, mac: String) {
        if (isFinished) return
        isFinished = true
        
        runOnUiThread {
            binding.statusText.text = "Measurement Complete"
            binding.dataFirstText.text = sys.toString()
            binding.titleFirstText.text = "SYS mmHg"
            
            binding.dataSecondText.text = dia.toString()
            binding.titleSecondText.text = "DIA mmHg"
            binding.secondDataLayout.visibility = View.VISIBLE
            
            binding.dataThirdText.text = pul.toString()
            binding.titleThirdText.text = "BPM"
            binding.thirdDataLayout.visibility = View.VISIBLE
            
            binding.progressBar.visibility = View.GONE
            binding.resultsContainer.visibility = View.VISIBLE
            binding.completeButton.visibility = View.VISIBLE
            
            // Save measurement
            preferencesUtils.updateDeviceData(mac, "$sys / $dia", pul.toString())
        }
    }
    
    override fun finishGlucose(mmol: Float, mac: String) {
        if (isFinished) return
        isFinished = true
        
        runOnUiThread {
            binding.statusText.text = "Measurement Complete"
            binding.dataFirstText.text = mmol.toInt().toString()
            binding.titleFirstText.text = "mg/dL"
            
            binding.secondDataLayout.visibility = View.GONE
            binding.thirdDataLayout.visibility = View.GONE
            
            binding.progressBar.visibility = View.GONE
            binding.resultsContainer.visibility = View.VISIBLE
            binding.completeButton.visibility = View.VISIBLE
            
            // Save measurement
            preferencesUtils.updateDeviceData(mac, "${mmol.toInt()} mg/dL", null)
        }
    }
    
    override fun finishWeight(weight: Double) {
        if (isFinished) return
        isFinished = true
        
        runOnUiThread {
            binding.statusText.text = "Measurement Complete"
            binding.dataFirstText.text = String.format("%.1f", weight)
            binding.titleFirstText.text = "lb"
            
            binding.secondDataLayout.visibility = View.GONE
            binding.thirdDataLayout.visibility = View.GONE
            
            binding.progressBar.visibility = View.GONE
            binding.resultsContainer.visibility = View.VISIBLE
            binding.completeButton.visibility = View.VISIBLE
            
            // Save measurement
            preferencesUtils.updateDeviceData(deviceMac, String.format("%.1f", weight), null)
        }
    }
    
    override fun onDeviceSelected() {
        runOnUiThread {
            binding.statusText.text = "Device detected, starting measurement"
            binding.progressBar.visibility = View.VISIBLE
        }
    }
    
    override fun disconnect() {
        runOnUiThread {
            if (!isFinished) {
                binding.statusText.text = "Device disconnected"
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bleManager.clearMeasureListener()
        if (!isFinished) {
            bleManager.disconnect(deviceMac)
        }
    }
}

