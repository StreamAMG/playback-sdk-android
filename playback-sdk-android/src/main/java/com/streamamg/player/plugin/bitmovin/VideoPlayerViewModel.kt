package com.streamamg.player.plugin.bitmovin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.source.SourceConfig
import com.streamamg.PlaybackSDKManager
import com.streamamg.player.plugin.VideoPlayerConfig
import com.streamamg.player.ui.BackgroundPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VideoPlayerViewModel : ViewModel() {
    var player: Player? = null
    private var currentVideoUrl: String? = null
    private var config: VideoPlayerConfig? = null
    private var isServiceBound = false
    private var backgroundPlaybackEnabled = false
    private var autoplayEnabled = false
    private var isPermissionsGranted = false
    private var _isPlayerReady = MutableStateFlow(false)
    val isPlayerReady: StateFlow<Boolean>
        get() = _isPlayerReady

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BackgroundPlaybackService.BackgroundBinder
            val playbackService = binder.getService()
            playbackService.setPlayer(player!!)  // Pass player to the service
            isServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isServiceBound = false
        }
    }

    fun initializePlayer(context: Context, config: VideoPlayerConfig, videoUrl: String) {
        this.config = config
        backgroundPlaybackEnabled = config.playbackConfig.backgroundPlaybackEnabled
        autoplayEnabled = config.playbackConfig.autoplayEnabled
        if (player == null || videoUrl != currentVideoUrl) {
            // Initialize player if not already initialized
            val playerConfig =
                PlayerConfig(key = PlaybackSDKManager.bitmovinLicense)
            player = Player(context, playerConfig)
        }
        loadVideo(videoUrl)

        updateBackgroundService(context)
    }

    private fun updateBackgroundService(context: Context) {
        if (backgroundPlaybackEnabled && isPermissionsGranted) {
            bindToBackgroundPlaybackService(context)
        }
    }

    fun updatePermissionsState(isGranted: Boolean, context: Context) {
        isPermissionsGranted = isGranted
        updateBackgroundService(context)
    }

    fun loadVideo(videoUrl: String) {
        currentVideoUrl = videoUrl
        val sourceConfig = SourceConfig.fromUrl(videoUrl)
        player?.pause()
        player?.load(sourceConfig)
        player?.next(PlayerEvent.Ready::class.java) {
            _isPlayerReady.value = true
        }
        player?.next(PlayerEvent.Error::class.java) {
            Log.d("SDK", "Player error")
        }
        if (autoplayEnabled) {
            player?.play()
        }

    }

    private fun bindToBackgroundPlaybackService(context: Context) {
        // Bind to the background service
        if (_isPlayerReady.value && !isServiceBound) {
            val intent = Intent(context, BackgroundPlaybackService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromService(context: Context) {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    fun handleAppInBackground(context: Context) {
        if (backgroundPlaybackEnabled) {
            bindToBackgroundPlaybackService(context)
        } else {
            player?.pause()
        }
    }

    fun handleAppInForeground(context: Context) {
        if (backgroundPlaybackEnabled) {
            unbindFromService(context)
        } else if (autoplayEnabled) {
            player?.play()
        }
    }

    fun playVideo() {
        if (isPlayerReady.value) {
            player?.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.destroy()
        player = null
    }
}