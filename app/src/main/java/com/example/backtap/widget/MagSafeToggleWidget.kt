package com.example.backtap.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.backtap.data.SettingsRepository
import com.example.backtap.di.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MagSafeToggleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Derive a Boolean flow directly from the app's DataStore.
        val isEnabledFlow = context.dataStore.data.map { prefs ->
            prefs[SettingsRepository.MASTER_TOGGLE] ?: false
        }

        provideContent {
            // collectAsState keeps the widget reactive — Glance re-composes
            // automatically whenever the DataStore value changes, including
            // changes made from the app UI, tile, or another widget instance.
            val isEnabled by isEnabledFlow.collectAsState(initial = false)

            val localContext = LocalContext.current
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(com.example.backtap.R.drawable.widget_background))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BACK TAP",
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily("sans-serif-medium")
                    ),
                    maxLines = 1
                )

                Image(
                    provider = ImageProvider(
                        if (isEnabled) com.example.backtap.R.drawable.ic_toggle_on
                        else com.example.backtap.R.drawable.ic_toggle_off
                    ),
                    contentDescription = "Toggle Back Tap",
                    modifier = GlanceModifier
                        .clickable(
                            actionSendBroadcast(
                                Intent(localContext, WidgetActionReceiver::class.java).apply {
                                    action = "com.example.backtap.WIDGET_TOGGLE"
                                }
                            )
                        )
                )
            }
        }
    }
}

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.backtap.WIDGET_TOGGLE") return

        // goAsync() keeps the receiver alive until the coroutine finishes,
        // preventing Android from killing it before the DataStore write completes.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // dataStore.edit {} is atomic/transactional, preventing race
                // conditions when the toggle is tapped rapidly.
                context.dataStore.edit { prefs ->
                    val current = prefs[SettingsRepository.MASTER_TOGGLE] ?: false
                    prefs[SettingsRepository.MASTER_TOGGLE] = !current
                }
                // updateAll triggers Glance to re-render all widget instances.
                MagSafeToggleWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class MagSafeToggleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MagSafeToggleWidget()
}
