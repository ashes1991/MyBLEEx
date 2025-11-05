package com.ble.kyv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ble.kyv.ble.Device
import com.ble.kyv.databinding.ItemDeviceBinding
import java.text.SimpleDateFormat
import java.util.*

class DevicesAdapter(
    private val onDeviceClick: (Device) -> Unit,
    private val onDeleteClick: (Device) -> Unit,
    private val onDeviceLongClick: (Device) -> Unit
) : ListAdapter<Device, DevicesAdapter.DeviceViewHolder>(DeviceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(device: Device) {
            binding.deviceName.text = device.name
            binding.deviceMac.text = device.mac
            
            // Set last measurement
            val measurementText = when {
                device.firstData != null && device.secondData != null -> {
                    "${device.firstData} / ${device.secondData}"
                }
                device.firstData != null -> device.firstData
                else -> "No measurements"
            }
            
            val dateText = device.date?.let {
                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                " - ${sdf.format(it)}"
            } ?: ""
            
            binding.lastMeasurement.text = "$measurementText$dateText"
            
            // Set device icon based on type
            val iconRes = when (device.type.name) {
                "BM1000C", "AD805" -> android.R.drawable.ic_dialog_info
                "U807", "LD575" -> android.R.drawable.ic_dialog_alert
                "WEIGHT" -> android.R.drawable.ic_menu_compass
                "CONTOUR" -> android.R.drawable.ic_menu_today
                else -> android.R.drawable.stat_sys_data_bluetooth
            }
            binding.deviceIcon.setImageResource(iconRes)
            
            // Click listeners
            binding.root.setOnClickListener {
                onDeviceClick(device)
            }
            
            binding.root.setOnLongClickListener {
                onDeviceLongClick(device)
                true
            }
            
            binding.deleteButton.setOnClickListener {
                onDeleteClick(device)
            }
        }
    }
    
    private class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.mac == newItem.mac
        }
        
        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}

