package com.example.backtap.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.datastore.preferences.core.edit
import com.example.backtap.R
import com.example.backtap.data.SettingsRepository
import com.example.backtap.di.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MagSafeToggleWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isEnabled = context.dataStore.data.map { prefs ->
                    prefs[SettingsRepository.MASTER_TOGGLE] ?: false
                }.first()

                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, isEnabled)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, isEnabled: Boolean) {
            val views = RemoteViews(context.packageName, R.layout.widget_back_tap_toggle)
            
            // Note: The TextView is already set to "Back Tap" in the layout file, 
            // but we can set it here too if needed.
            
            views.setImageViewResource(
                R.id.widget_switch,
                if (isEnabled) R.drawable.ic_switch_on else R.drawable.ic_switch_off
            )

            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = "com.example.backtap.WIDGET_TOGGLE"
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            views.setOnClickPendingIntent(R.id.widget_switch, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.backtap.WIDGET_TOGGLE") return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Toggle the state atomically
                context.dataStore.edit { prefs ->
                    val current = prefs[SettingsRepository.MASTER_TOGGLE] ?: false
                    prefs[SettingsRepository.MASTER_TOGGLE] = !current
                }

                // Retrieve the updated state
                val isEnabled = context.dataStore.data.map { prefs ->
                    prefs[SettingsRepository.MASTER_TOGGLE] ?: false
                }.first()

                // Update all widget instances
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MagSafeToggleWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                for (appWidgetId in appWidgetIds) {
                    MagSafeToggleWidgetReceiver.updateAppWidget(context, appWidgetManager, appWidgetId, isEnabled)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
