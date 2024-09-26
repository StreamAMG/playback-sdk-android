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
import java.net.URL

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
            playbackService.setPlayer(player!!)
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
        if (player == null) {
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
        if (!urlsAreEqualExcludingKs(currentVideoUrl ?: "", videoUrl)) {
            val sourceConfig = SourceConfig.fromUrl(videoUrl)
            player?.load(sourceConfig)
        }
        currentVideoUrl = videoUrl
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

    private fun urlsAreEqualExcludingKs(url1: String, url2: String): Boolean {
        if (url1.isEmpty()) return false
        if (url2.isEmpty()) return false
        val normalizedUrl1 = normalizeUrl(url1)
        val normalizedUrl2 = normalizeUrl(url2)
        return normalizedUrl1 == normalizedUrl2
    }

    private fun normalizeUrl(urlString: String): String {
        val url = URL(urlString)
        val protocol = url.protocol
        val host = url.host
        val port = url.port
        val path = url.path

        // Parse query parameters excluding 'ks' and sort them for consistent comparison
        val queryParams = url.query?.split("&")?.mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts[0] != "ks") {
                it
            } else null
        }?.sorted()?.joinToString("&") ?: ""

        // Reconstruct the URL without the 'ks' parameter
        val normalizedUrl = StringBuilder()
        normalizedUrl.append(protocol).append("://").append(host)
        if (port != -1) {
            normalizedUrl.append(":").append(port)
        }
        normalizedUrl.append(path)
        if (queryParams.isNotEmpty()) {
            normalizedUrl.append("?").append(queryParams)
        }
        return normalizedUrl.toString()
    }
}