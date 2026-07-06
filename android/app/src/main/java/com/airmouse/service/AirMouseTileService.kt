package com.airmouse.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.airmouse.network.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AirMouseTileService : TileService() {

    @Inject
    lateinit var connectionManager: ConnectionManager

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isEnabled = connectionManager.isMouseControlEnabled
        connectionManager.isMouseControlEnabled = !isEnabled
        updateTileState()
        
        if (!isEnabled) {
            val intent = android.content.Intent(this, com.airmouse.presentation.MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("open_touchpad", true)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isEnabled = connectionManager.isMouseControlEnabled
        val isConnected = connectionManager.isConnected()

        if (isConnected) {
            if (isEnabled) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Air Mouse: ON"
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Air Mouse: Paused"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Air Mouse"
        }
        
        tile.updateTile()
    }
}
