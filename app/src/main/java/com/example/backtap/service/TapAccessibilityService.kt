package com.example.backtap.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.backtap.data.SettingsRepository
import com.example.backtap.engine.GestureRecognitionEngine
import com.example.backtap.receiver.SystemStateReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class TapAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var engine: GestureRecognitionEngine

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var doubleTapAction = "NONE"
    private var tripleTapAction = "NONE"
    private var isMasterToggleOn = true
    private var isFlashlightOn = false

    private lateinit var systemStateReceiver: SystemStateReceiver
    private lateinit var cameraManager: android.hardware.camera2.CameraManager

    private val torchCallback = object : android.hardware.camera2.CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isFlashlightOn = enabled
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("TapService", "Service Connected")

        cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        cameraManager.registerTorchCallback(torchCallback, android.os.Handler(android.os.Looper.getMainLooper()))

        systemStateReceiver = SystemStateReceiver { shouldRun ->
            if (shouldRun && isMasterToggleOn) {
                engine.start()
            } else {
                engine.stop()
            }
        }
        
        registerReceiver(systemStateReceiver, systemStateReceiver.getIntentFilter())

        // Observe Settings
        serviceScope.launch {
            settingsRepository.isMasterToggleOn.collectLatest { isOn ->
                isMasterToggleOn = isOn
                if (isOn) engine.start() else engine.stop()
            }
        }

        serviceScope.launch {
            settingsRepository.sensitivityThreshold.collectLatest { threshold ->
                engine.sensitivityThreshold = threshold
            }
        }

        serviceScope.launch {
            settingsRepository.pauseBelow10Percent.collectLatest { pause ->
                systemStateReceiver.pauseBelow10Percent = pause
            }
        }

        serviceScope.launch {
            settingsRepository.doubleTapAction.collectLatest { doubleTapAction = it }
        }

        serviceScope.launch {
            settingsRepository.tripleTapAction.collectLatest { tripleTapAction = it }
        }

        // Observe Gestures
        serviceScope.launch {
            engine.gestureEvents.collectLatest { event ->
                if (!isMasterToggleOn) return@collectLatest
                when (event) {
                    GestureRecognitionEngine.GestureEvent.DOUBLE_TAP -> executeAction(doubleTapAction)
                    GestureRecognitionEngine.GestureEvent.TRIPLE_TAP -> executeAction(tripleTapAction)
                }
            }
        }
    }

    private fun executeAction(action: String) {
        Log.d("TapService", "Executing action: $action")
        
        // Trigger light haptic feedback on successful action
        if (action != "NONE") {
            try {
                val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(20)
                    }
                }
            } catch (e: Exception) {
                Log.e("TapService", "Haptic feedback failed: ${e.message}")
            }
        }

        when (action) {
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "QUICK_SETTINGS" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "LOCK_SCREEN" -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            "TAKE_SCREENSHOT" -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            "PLAY_PAUSE" -> dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "NEXT_TRACK" -> dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            "PREVIOUS_TRACK" -> dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "VOLUME_UP" -> adjustVolume(android.media.AudioManager.ADJUST_RAISE)
            "VOLUME_DOWN" -> adjustVolume(android.media.AudioManager.ADJUST_LOWER)
            "MUTE_UNMUTE" -> adjustVolume(android.media.AudioManager.ADJUST_TOGGLE_MUTE)
            "TOGGLE_FLASHLIGHT" -> toggleFlashlight()
            "CYCLE_RINGER" -> cycleRingerMode()
            "NONE" -> { /* Do nothing */ }
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, !isFlashlightOn)
            }
        } catch (e: Exception) {
            Log.e("TapService", "Failed to toggle flashlight: ${e.message}")
        }
    }

    private fun cycleRingerMode() {
        try {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            when (audioManager.ringerMode) {
                android.media.AudioManager.RINGER_MODE_NORMAL -> audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
                android.media.AudioManager.RINGER_MODE_VIBRATE -> {
                    try {
                        audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
                    } catch (e: SecurityException) {
                        // Fallback to normal if Do Not Disturb access is not granted
                        audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                    }
                }
                android.media.AudioManager.RINGER_MODE_SILENT -> audioManager.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
            }
        } catch (e: Exception) {
            Log.e("TapService", "Failed to cycle ringer mode: ${e.message}")
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, android.media.AudioManager.FLAG_SHOW_UI)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for our purpose
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        engine.stop()
        unregisterReceiver(systemStateReceiver)
        if (::cameraManager.isInitialized) {
            cameraManager.unregisterTorchCallback(torchCallback)
        }
    }
}
