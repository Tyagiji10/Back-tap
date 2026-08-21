package com.example.backtap.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.backtap.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackTapTileService : TileService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var stateCollectionJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        
        stateCollectionJob?.cancel()
        stateCollectionJob = serviceScope.launch {
            settingsRepository.isMasterToggleOn.collect { isEnabled ->
                val tile = qsTile ?: return@collect
                tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = "Back Tap"
                tile.updateTile()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        stateCollectionJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentState = settingsRepository.isMasterToggleOn.first()
            settingsRepository.setMasterToggle(!currentState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
