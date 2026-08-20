package com.example.backtap.engine

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class GestureRecognitionEngine @Inject constructor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private val engineScope = CoroutineScope(Dispatchers.Default + Job())

    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var gyroscope: Sensor? = null

    // State
    private var isEngineRunning = false
    private var currentProximity = -1f // -1 means unknown or not covered yet.
    private var wasFlat = false
    private var currentGyroX = 0f
    private var currentGyroY = 0f
    private var currentGyroZ = 0f
    
    // Config
    var sensitivityThreshold = 2.5f
    var maxTapThreshold = 9.5f
    var shakeThreshold = 4.5f
    private val GRAVITY = 9.8f
    private val TABLE_CHECK_MARGIN = 0.5f
    private val GYRO_SHOCK_THRESHOLD = 2.2f

    // Window config
    private val DOUBLE_TAP_WINDOW_MS = 400L
    private val TRIPLE_TAP_WINDOW_MS = 700L
    private val DEBOUNCE_MS = 150L
    private val COOLDOWN_MS = 500L
    private val HARD_COLLISION_COOLDOWN_MS = 400L

    // Heuristic State
    private var lastPeakTime = 0L
    private var peakCount = 0
    private var firstPeakTime = 0L
    private var cooldownUntil = 0L

    // Events
    private val _gestureEvents = MutableSharedFlow<GestureEvent>()
    val gestureEvents = _gestureEvents.asSharedFlow()

    enum class GestureEvent {
        DOUBLE_TAP,
        TRIPLE_TAP
    }

    fun start() {
        if (isEngineRunning) return
        
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        
        isEngineRunning = true
        Log.d("GestureEngine", "Engine started")
    }

    fun stop() {
        if (!isEngineRunning) return
        sensorManager.unregisterListener(this)
        isEngineRunning = false
        Log.d("GestureEngine", "Engine stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isEngineRunning) return

        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            currentProximity = event.values[0]
            return
        }

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            currentGyroX = event.values[0]
            currentGyroY = event.values[1]
            currentGyroZ = event.values[2]
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Proximity Check
            if (currentProximity == 0.0f) {
                return // In pocket or face down, ignore.
            }

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Table Check: Ignore if Z-axis is steady near 9.8 or -9.8 and X/Y near 0
            if (abs(abs(z) - GRAVITY) < TABLE_CHECK_MARGIN && abs(x) < 2f && abs(y) < 2f) {
                // Device is highly likely resting flat on a table.
                if (!wasFlat) {
                    wasFlat = true
                    resetTapBuffer()
                }
                return
            } else if (wasFlat) {
                // Device picked up
                wasFlat = false
                resetTapBuffer()
            }

            // High-pass filter conceptually: we want sudden changes, but simple baseline works too
            // Assuming Z axis is roughly facing the user, a tap pushes the phone forward (-Z) then it springs back (+Z)
            // A simple delta check works as a Stage 1 heuristic.
            
            // Note: If held upright, Z is roughly 0. If held flat, Z is 9.8.
            // But we don't know orientation, so we look for *sudden* deltas across subsequent readings, or 
            // we can look at magnitude. But wait, delta over time is better than delta from gravity, 
            // since gravity applies constantly on whatever axis is pointing down.
            // Let's implement a simple rolling average for baseline, or just use the delta between events.
            
            // To be accurate with the prompt's suggested `val zAxisDelta = Math.abs(zAxisAcceleration - GRAVITY - baselineZ)`,
            // since we don't have baselineZ immediately, we'll do a simple low-pass to find baseline, then check delta.
            
            val currentX = x
            val currentY = y
            val currentZ = z
            updateBaselines(currentX, currentY, currentZ)
            
            val now = System.currentTimeMillis()
            if (now < cooldownUntil) {
                return
            }

            val xDelta = abs(currentX - baselineX)
            val yDelta = abs(currentY - baselineY)

            if (xDelta > shakeThreshold || yDelta > shakeThreshold) {
                // Device is shaking. Clear tap buffer, set a cooldown, and abort.
                resetTapBuffer()
                cooldownUntil = now + COOLDOWN_MS
                return
            }

            // 1. Directional Filtering (Positive delta only)
            val zAxisDelta = currentZ - baselineZ

            if (zAxisDelta > sensitivityThreshold) {
                if (zAxisDelta > maxTapThreshold) {
                    resetTapBuffer()
                    cooldownUntil = now + HARD_COLLISION_COOLDOWN_MS
                    return
                }

                val gyroMagnitude = kotlin.math.sqrt((currentGyroX * currentGyroX + currentGyroY * currentGyroY + currentGyroZ * currentGyroZ).toDouble()).toFloat()
                if (gyroMagnitude > GYRO_SHOCK_THRESHOLD) {
                    resetTapBuffer()
                    cooldownUntil = now + HARD_COLLISION_COOLDOWN_MS
                    return
                }
                
                // Discard old taps before adding new ones
                if (peakCount > 0 && (now - firstPeakTime) > TRIPLE_TAP_WINDOW_MS) {
                    resetTapBuffer()
                }

                // Debounce
                if (now - lastPeakTime < DEBOUNCE_MS) {
                    return
                }

                lastPeakTime = now
                Log.d("GestureEngine", "Peak detected! Delta: $zAxisDelta")

                if (peakCount == 0) {
                    firstPeakTime = now
                    peakCount = 1
                } else {
                    peakCount++
                    val timeSinceFirst = now - firstPeakTime

                    if (peakCount == 2) {
                        if (timeSinceFirst <= DOUBLE_TAP_WINDOW_MS) {
                            // Could be a double tap, wait a bit for a triple tap
                            engineScope.launch {
                                kotlinx.coroutines.delay(TRIPLE_TAP_WINDOW_MS - timeSinceFirst + 50)
                                evaluateTaps()
                            }
                        } else {
                            // Too late for double tap, reset window starting now
                            firstPeakTime = now
                            peakCount = 1
                        }
                    } else if (peakCount == 3) {
                        if (timeSinceFirst <= TRIPLE_TAP_WINDOW_MS) {
                            // Definitely a triple tap
                            emitGesture(GestureEvent.TRIPLE_TAP)
                            peakCount = 0 // Reset
                        } else {
                            // Too late for triple tap.
                            firstPeakTime = now
                            peakCount = 1
                        }
                    }
                }
            }
        }
    }

    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = GRAVITY
    private val ALPHA = 0.8f // Low pass filter constant

    private fun updateBaselines(currentX: Float, currentY: Float, currentZ: Float) {
        baselineX = ALPHA * baselineX + (1 - ALPHA) * currentX
        baselineY = ALPHA * baselineY + (1 - ALPHA) * currentY
        baselineZ = ALPHA * baselineZ + (1 - ALPHA) * currentZ
    }

    private fun evaluateTaps() {
        if (peakCount == 2) {
            val now = System.currentTimeMillis()
            if (now - firstPeakTime > TRIPLE_TAP_WINDOW_MS) {
                // No third tap arrived in time. It's a double tap.
                emitGesture(GestureEvent.DOUBLE_TAP)
                resetTapBuffer()
            }
        }
    }

    private fun resetTapBuffer() {
        peakCount = 0
        firstPeakTime = 0L
        lastPeakTime = 0L
    }

    private fun emitGesture(event: GestureEvent) {
        engineScope.launch {
            Log.d("GestureEngine", "Emitting gesture: $event")
            _gestureEvents.emit(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
