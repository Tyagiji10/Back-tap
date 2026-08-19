package com.example.backtap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backtap.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isMasterToggleOn: StateFlow<Boolean> = settingsRepository.isMasterToggleOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sensitivityThreshold: StateFlow<Float> = settingsRepository.sensitivityThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_SENSITIVITY)

    val pauseBelow10Percent: StateFlow<Boolean> = settingsRepository.pauseBelow10Percent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val doubleTapAction: StateFlow<String> = settingsRepository.doubleTapAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NONE")

    val tripleTapAction: StateFlow<String> = settingsRepository.tripleTapAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NONE")

    fun setMasterToggle(value: Boolean) = viewModelScope.launch {
        settingsRepository.setMasterToggle(value)
    }

    fun setSensitivity(value: Float) = viewModelScope.launch {
        settingsRepository.setSensitivity(value)
    }

    fun setPauseBelow10Percent(value: Boolean) = viewModelScope.launch {
        settingsRepository.setPauseBelow10Percent(value)
    }

    fun setDoubleTapAction(action: String) = viewModelScope.launch {
        settingsRepository.setDoubleTapAction(action)
    }

    fun setTripleTapAction(action: String) = viewModelScope.launch {
        settingsRepository.setTripleTapAction(action)
    }
}
