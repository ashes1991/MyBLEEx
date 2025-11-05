package com.ble.kyv

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ble.kyv.databinding.ItemScannedDeviceBinding

class ScannedDevicesAdapter(
    private val onDeviceClick: (ScanResult) -> Unit
) : ListAdapter<ScanResult, ScannedDevicesAdapter.ScannedDeviceViewHolder>(ScanResultDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedDeviceViewHolder {
        val binding = ItemScannedDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScannedDeviceViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ScannedDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ScannedDeviceViewHolder(
        private val binding: ItemScannedDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(scanResult: ScanResult) {
            binding.scannedDeviceName.text = scanResult.device.name ?: "Unknown Device"
            binding.scannedDeviceMac.text = scanResult.device.address
            binding.scannedDeviceRssi.text = "RSSI: ${scanResult.rssi} dBm"
            
            binding.root.setOnClickListener {
                onDeviceClick(scanResult)
            }
        }
    }
    
    private class ScanResultDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem.device.address == newItem.device.address
        }
        
        override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem.rssi == newItem.rssi
        }
    }
}

