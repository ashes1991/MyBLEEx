package com.ble.kyv

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ble.kyv.ble.BLEEvent
import com.ble.kyv.databinding.ItemBleEventBinding

class BLEEventsAdapter : ListAdapter<BLEEvent, BLEEventsAdapter.EventViewHolder>(EventDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemBleEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class EventViewHolder(
        private val binding: ItemBleEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(event: BLEEvent) {
            binding.eventTime.text = event.getShortTimestamp()
            binding.eventType.text = event.eventType.name
            binding.eventMessage.text = event.message
            
            // Set color based on event type
            val color = getColorForEventType(event.eventType)
            binding.eventTypeIndicator.setBackgroundColor(color)
            binding.eventType.setBackgroundColor(color)
            
            // Show details if available
            if (!event.details.isNullOrEmpty()) {
                binding.eventDetails.text = event.details
                binding.eventDetails.visibility = View.VISIBLE
            } else {
                binding.eventDetails.visibility = View.GONE
            }
        }
        
        private fun getColorForEventType(eventType: BLEEvent.EventType): Int {
            return when (eventType) {
                BLEEvent.EventType.SCAN_FOUND -> Color.parseColor("#2196F3") // Blue
                BLEEvent.EventType.CONNECTING -> Color.parseColor("#FF9800") // Orange
                BLEEvent.EventType.CONNECTED -> Color.parseColor("#4CAF50") // Green
                BLEEvent.EventType.DISCONNECTED -> Color.parseColor("#9E9E9E") // Grey
                BLEEvent.EventType.ERROR -> Color.parseColor("#F44336") // Red
                BLEEvent.EventType.DATA_RECEIVED -> Color.parseColor("#00BCD4") // Cyan
                BLEEvent.EventType.MEASUREMENT_START -> Color.parseColor("#9C27B0") // Purple
                BLEEvent.EventType.MEASUREMENT_PROGRESS -> Color.parseColor("#673AB7") // Deep Purple
                BLEEvent.EventType.MEASUREMENT_COMPLETE -> Color.parseColor("#4CAF50") // Green
                BLEEvent.EventType.SERVICE_DISCOVERED -> Color.parseColor("#3F51B5") // Indigo
                BLEEvent.EventType.CHARACTERISTIC_READ -> Color.parseColor("#03A9F4") // Light Blue
                BLEEvent.EventType.CHARACTERISTIC_WRITE -> Color.parseColor("#00BCD4") // Cyan
            }
        }
    }
    
    private class EventDiffCallback : DiffUtil.ItemCallback<BLEEvent>() {
        override fun areItemsTheSame(oldItem: BLEEvent, newItem: BLEEvent): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.deviceMac == newItem.deviceMac
        }
        
        override fun areContentsTheSame(oldItem: BLEEvent, newItem: BLEEvent): Boolean {
            return oldItem == newItem
        }
    }
}

