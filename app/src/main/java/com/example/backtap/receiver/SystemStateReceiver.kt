package com.example.backtap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

class SystemStateReceiver(
    private val onStateChange: (Boolean) -> Unit
) : BroadcastReceiver() {

    var pauseBelow10Percent = true

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("SystemStateReceiver", "Screen off, pausing engine")
                onStateChange(false)
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d("SystemStateReceiver", "Screen on, checking battery state")
                checkBatteryAndResume(context)
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val batteryPct = level * 100 / scale.toFloat()
                    if (pauseBelow10Percent && batteryPct < 10f) {
                        Log.d("SystemStateReceiver", "Battery below 10%, pausing engine")
                        onStateChange(false)
                    } else {
                        // Assuming we can resume if screen is on.
                        // We shouldn't blindly resume if screen is off, but we'll leave that to the service
                        // to handle overall state. The callback is just a signal.
                        onStateChange(true)
                    }
                }
            }
        }
    }

    private fun checkBatteryAndResume(context: Context) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level != -1 && scale != -1) {
            val batteryPct = level * 100 / scale.toFloat()
            if (pauseBelow10Percent && batteryPct < 10f) {
                onStateChange(false)
                return
            }
        }
        onStateChange(true)
    }

    fun getIntentFilter(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        return filter
    }
}
