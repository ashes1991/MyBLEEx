package com.ble.kyv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ble.kyv.ble.BLELogManager
import com.ble.kyv.databinding.ActivityDeviceLogsBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DeviceLogsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDeviceLogsBinding
    private lateinit var logManager: BLELogManager
    private lateinit var eventsAdapter: BLEEventsAdapter
    
    private var deviceName: String = ""
    private var deviceMac: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get device info from intent
        deviceName = intent.getStringExtra("device_name") ?: "Unknown Device"
        deviceMac = intent.getStringExtra("device_mac") ?: ""
        
        if (deviceMac.isEmpty()) {
            finish()
            return
        }
        
        logManager = BLELogManager(this)
        
        setupViews()
        setupRecyclerView()
        loadLogs()
    }
    
    private fun setupViews() {
        binding.toolbarTitle.text = "Logs: $deviceName"
        binding.deviceNameText.text = deviceName
        binding.deviceMacText.text = deviceMac
        
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.shareLogsButton.setOnClickListener {
            shareLogsAsFile()
        }
        
        binding.clearLogsButton.setOnClickListener {
            showClearLogsConfirmation()
        }
    }
    
    private fun setupRecyclerView() {
        eventsAdapter = BLEEventsAdapter()
        binding.logsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DeviceLogsActivity)
            adapter = eventsAdapter
        }
    }
    
    private fun loadLogs() {
        val logs = logManager.getLogsForDevice(deviceMac)
        
        // Sort by timestamp ascending (oldest first - chronological order)
        val sortedLogs = logs.sortedBy { it.timestamp }
        
        binding.logsCountText.text = "Total events: ${logs.size}"
        
        if (sortedLogs.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.logsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.logsRecyclerView.visibility = View.VISIBLE
            eventsAdapter.submitList(sortedLogs)
            
        }
    }
    
    private fun showClearLogsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Logs")
            .setMessage("Are you sure you want to clear all logs for this device?")
            .setPositiveButton("Clear") { _, _ ->
                logManager.clearLogsForDevice(deviceMac)
                loadLogs()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun shareLogsAsFile() {
        val logs = logManager.getLogsForDevice(deviceMac)
        
        if (logs.isEmpty()) {
            Toast.makeText(this, "No logs to share", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Create logs text content
            val logsContent = buildLogsText(logs)
            
            // Create file in cache directory
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "BLE_Logs_${deviceName.replace(" ", "_")}_$timestamp.txt"
            val file = File(cacheDir, fileName)
            
            // Write logs to file
            file.writeText(logsContent)
            
            // Create share intent
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "BLE Logs: $deviceName")
                putExtra(Intent.EXTRA_TEXT, "BLE event logs for device: $deviceName ($deviceMac)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share logs via"))
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing logs: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun buildLogsText(logs: List<com.ble.kyv.ble.BLEEvent>): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("=======================================")
        sb.appendLine("BLE DEVICE LOGS")
        sb.appendLine("=======================================")
        sb.appendLine()
        sb.appendLine("Device Name: $deviceName")
        sb.appendLine("Device MAC: $deviceMac")
        sb.appendLine("Total Events: ${logs.size}")
        sb.appendLine("Export Date: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine()
        sb.appendLine("=======================================")
        sb.appendLine("EVENT LOG")
        sb.appendLine("=======================================")
        sb.appendLine()
        
        // Sort by timestamp (chronological order)
        val sortedLogs = logs.sortedBy { it.timestamp }
        
        // Add each event
        sortedLogs.forEachIndexed { index, event ->
            sb.appendLine("[${index + 1}] ${event.getFormattedTimestamp()}")
            sb.appendLine("Type: ${event.eventType}")
            sb.appendLine("Message: ${event.message}")
            if (!event.details.isNullOrEmpty()) {
                sb.appendLine("Details: ${event.details}")
            }
            sb.appendLine()
        }
        
        // Footer
        sb.appendLine("=======================================")
        sb.appendLine("END OF LOG")
        sb.appendLine("=======================================")
        
        return sb.toString()
    }
}

