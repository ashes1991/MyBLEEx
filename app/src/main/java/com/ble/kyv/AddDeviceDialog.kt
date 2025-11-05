package com.ble.kyv

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ble.kyv.ble.Device
import com.ble.kyv.ble.DeviceType
import com.google.android.material.textfield.TextInputEditText

class AddDeviceDialog(
    private val context: Context,
    private val onDeviceAdded: (Device) -> Unit
) {
    
    private var dialog: AlertDialog? = null
    
    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_device, null)
        
        val deviceNameInput = dialogView.findViewById<TextInputEditText>(R.id.deviceNameInput)
        val macAddressInput = dialogView.findViewById<TextInputEditText>(R.id.macAddressInput)
        val deviceTypeSpinner = dialogView.findViewById<Spinner>(R.id.deviceTypeSpinner)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)
        
        // Setup device type spinner
        val deviceTypes = DeviceType.values().filter { it != DeviceType.UNKNOWN }
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            deviceTypes.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceTypeSpinner.adapter = adapter
        
        dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        cancelButton.setOnClickListener {
            dialog?.dismiss()
        }
        
        addButton.setOnClickListener {
            val deviceName = deviceNameInput.text.toString().trim()
            val macAddress = macAddressInput.text.toString().trim().uppercase()
            val selectedDeviceType = deviceTypes[deviceTypeSpinner.selectedItemPosition]
            
            if (validateInput(deviceName, macAddress)) {
                val device = Device(
                    name = deviceName,
                    mac = macAddress,
                    type = selectedDeviceType
                )
                onDeviceAdded(device)
                dialog?.dismiss()
            }
        }
        
        dialog?.show()
    }
    
    private fun validateInput(name: String, mac: String): Boolean {
        return when {
            name.isEmpty() -> {
                Toast.makeText(context, "Please enter device name", Toast.LENGTH_SHORT).show()
                false
            }
            mac.isEmpty() -> {
                Toast.makeText(context, "Please enter MAC address", Toast.LENGTH_SHORT).show()
                false
            }
            !isValidMacAddress(mac) -> {
                Toast.makeText(context, "Invalid MAC address format", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
    
    private fun isValidMacAddress(mac: String): Boolean {
        val macPattern = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$".toRegex()
        return macPattern.matches(mac)
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
}

