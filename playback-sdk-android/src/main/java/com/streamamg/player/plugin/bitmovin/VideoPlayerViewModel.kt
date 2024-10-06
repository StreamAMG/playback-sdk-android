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
import java.net.MalformedURLException
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
        unbindFromService(context)

        loadVideo(videoUrl)

        updateBackgroundService(context)
    }

     fun updateBackgroundService(context: Context) {
        if (backgroundPlaybackEnabled && isPermissionsGranted) {
            bindToBackgroundPlaybackService(context)
        }
    }

    fun updatePermissionsState(isGranted: Boolean, context: Context) {
        isPermissionsGranted = isGranted
        updateBackgroundService(context)
    }

    private fun loadVideo(videoUrl: String) {
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
        try {
            if (isServiceBound) {
                context.unbindService(serviceConnection)
                isServiceBound = false
            }
        } catch (_: IllegalArgumentException) {}
    }

    fun unbindAndStopService(context: Context) {
        val intent = Intent(context, BackgroundPlaybackService::class.java)
        try {
            if (isServiceBound) {
                context.unbindService(serviceConnection)
                isServiceBound = false
            }
            context.stopService(intent)
        } catch (_: IllegalArgumentException) {}
    }

    fun handleAppInBackground(context: Context) {
        if (backgroundPlaybackEnabled && _isPlayerReady.value) {
            bindToBackgroundPlaybackService(context)
        } else {
            player?.pause()
        }
    }

    fun handleAppInForeground(context: Context) {
        if (backgroundPlaybackEnabled && _isPlayerReady.value) {
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

    fun pauseVideo() {
        if (isPlayerReady.value) {
            player?.pause()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.destroy()
        player = null
    }

    private fun urlsAreEqualExcludingKs(url1: String, url2: String): Boolean {
        if (url1.isEmpty() || url2.isEmpty()) return false

        return try {
            val normalizedUrl1 = normalizeUrl(url1)
            val normalizedUrl2 = normalizeUrl(url2)
            normalizedUrl1 == normalizedUrl2
        } catch (e: MalformedURLException) {
            // Якщо URL некоректний, повертаємо false
            e.printStackTrace()
            false
        } catch (e: Exception) {
            // Обробляємо будь-які інші непередбачені винятки
            e.printStackTrace()
            false
        }
    }

    private fun normalizeUrl(urlString: String): String {
        return try {
            val url = URL(urlString)

            val protocol = url.protocol ?: return urlString
            val host = url.host ?: return urlString
            val port = url.port
            val path = url.path ?: ""

            val queryParams = url.query?.split("&")?.mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts[0] != "ks") {
                    it
                } else null
            }?.sorted()?.joinToString("&") ?: ""

            val normalizedUrl = StringBuilder()
            normalizedUrl.append(protocol).append("://").append(host)
            if (port != -1) {
                normalizedUrl.append(":").append(port)
            }
            normalizedUrl.append(path)
            if (queryParams.isNotEmpty()) {
                normalizedUrl.append("?").append(queryParams)
            }
            normalizedUrl.toString()

        } catch (e: MalformedURLException) {
            e.printStackTrace()
            urlString
        } catch (e: Exception) {
            e.printStackTrace()
            urlString
        }
    }
}