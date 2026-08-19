package com.example.backtap.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val MASTER_TOGGLE = booleanPreferencesKey("master_toggle")
        val SENSITIVITY_THRESHOLD = floatPreferencesKey("sensitivity_threshold")
        val PAUSE_BELOW_10_PERCENT = booleanPreferencesKey("pause_below_10_percent")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
        val TRIPLE_TAP_ACTION = stringPreferencesKey("triple_tap_action")

        const val DEFAULT_SENSITIVITY = 6.0f
    }

    val isMasterToggleOn: Flow<Boolean> = dataStore.data.map { it[MASTER_TOGGLE] ?: false }
    val sensitivityThreshold: Flow<Float> = dataStore.data.map { it[SENSITIVITY_THRESHOLD] ?: DEFAULT_SENSITIVITY }
    val pauseBelow10Percent: Flow<Boolean> = dataStore.data.map { it[PAUSE_BELOW_10_PERCENT] ?: true }
    val doubleTapAction: Flow<String> = dataStore.data.map { it[DOUBLE_TAP_ACTION] ?: "NONE" }
    val tripleTapAction: Flow<String> = dataStore.data.map { it[TRIPLE_TAP_ACTION] ?: "NONE" }

    suspend fun setMasterToggle(value: Boolean) {
        dataStore.edit { it[MASTER_TOGGLE] = value }
    }

    suspend fun setSensitivity(value: Float) {
        dataStore.edit { it[SENSITIVITY_THRESHOLD] = value }
    }

    suspend fun setPauseBelow10Percent(value: Boolean) {
        dataStore.edit { it[PAUSE_BELOW_10_PERCENT] = value }
    }

    suspend fun setDoubleTapAction(action: String) {
        dataStore.edit { it[DOUBLE_TAP_ACTION] = action }
    }

    suspend fun setTripleTapAction(action: String) {
        dataStore.edit { it[TRIPLE_TAP_ACTION] = action }
    }
}
