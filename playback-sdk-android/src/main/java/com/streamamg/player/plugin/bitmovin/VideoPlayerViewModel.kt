package com.streamamg.player.plugin.bitmovin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.streamamg.PlaybackSDKManager
import com.streamamg.api.player.PlaybackVideoDetails
import com.streamamg.player.plugin.VideoPlayerConfig
import com.streamamg.player.ui.BackgroundPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.MalformedURLException
import java.net.URL

class VideoPlayerViewModel : ViewModel() {
    var player: Player? = null
    private var sources: List<Source> = emptyList()
    private var currentVideoDetail: PlaybackVideoDetails? = null
    private var currentVideoDetails: Array<PlaybackVideoDetails>? = null
    private var entryIDToPlay: String? = null
    private var authorizationToken: String? = null
    private var config: VideoPlayerConfig? = null
    private var playerConfig: PlayerConfig? = null
    private var isServiceBound = false
    private var backgroundPlaybackEnabled = false
    private var autoplayEnabled = false
    private var isPermissionsGranted = false
    private var isPlayerPaused = false
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

    fun initializePlayer(context: Context, config: VideoPlayerConfig, videoDetails: Array<PlaybackVideoDetails>, playerConfig: PlayerConfig?, entryIDToPlay: String?, authorizationToken: String?, analyticsViewerId: String? = null) {
        this.config = config
        backgroundPlaybackEnabled = config.playbackConfig.backgroundPlaybackEnabled
        autoplayEnabled = config.playbackConfig.autoplayEnabled
        this.entryIDToPlay = entryIDToPlay
        this.authorizationToken = authorizationToken
        if (player == null) {
            playerConfig?.let {
                this.playerConfig = PlayerConfig(
                    key = PlaybackSDKManager.bitmovinLicense,
                    styleConfig = it.styleConfig,
                    playbackConfig = it.playbackConfig,
                    licensingConfig = it.licensingConfig,
                    advertisingConfig = it.advertisingConfig,
                    remoteControlConfig = it.remoteControlConfig,
                    adaptationConfig = it.adaptationConfig,
                    networkConfig = it.networkConfig,
                    liveConfig = it.liveConfig,
                    tweaksConfig = it.tweaksConfig,
                    bufferConfig = it.bufferConfig
                    )
            } ?: run {
                this.playerConfig = PlayerConfig(key = PlaybackSDKManager.bitmovinLicense)
            }
            val analyticsConfig = provideAnalyticsConfig(PlaybackSDKManager.analyticsLicense, analyticsViewerId)
            player = this.playerConfig?.let { Player(context, it, analyticsConfig) }
        }
        unbindFromService(context)

        loadPlaylist(videoDetails)

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

    private fun provideAnalyticsConfig(license: String? = null, analyticsViewerId: String?): AnalyticsPlayerConfig {
        return license?.let {
            val config = AnalyticsConfig.Builder(it).build()
            AnalyticsPlayerConfig.Enabled(config, DefaultMetadata(customUserId = analyticsViewerId))
        } ?: AnalyticsPlayerConfig.Disabled
    }

    private fun loadVideo(videoDetails: PlaybackVideoDetails) {
        if (!urlsAreEqualExcludingKs(currentVideoDetail?.url ?: "", videoDetails.url ?: "")) {
            val sourceConfig = SourceConfig.fromUrl(currentVideoDetail?.url ?: "")
            isPlayerPaused = false
            player?.load(sourceConfig)
        }
        currentVideoDetail = videoDetails
        player?.next(PlayerEvent.Ready::class.java) {
            _isPlayerReady.value = true
        }
        player?.next(PlayerEvent.Error::class.java) {
            Log.d("SDK", "Player error ${it.message}")
        }
        player?.next(PlayerEvent.Paused::class) {
            isPlayerPaused = player?.isPaused == true
        }
        player?.next(PlayerEvent.Play::class) {
            isPlayerPaused = player?.isPaused == true
        }
        if (autoplayEnabled && !isPlayerPaused) {
            player?.play()
        }
    }

    private fun loadPlaylist(videoDetails: Array<PlaybackVideoDetails>) {
        if (!currentVideoDetails.contentEquals(videoDetails)) {
            isPlayerPaused = false
            sources = createPlaylist(videoDetails)
            if (sources.isEmpty()) return
            val playlistOptions = PlaylistOptions(preloadAllSources = false)
            val playlistConfig = PlaylistConfig(sources, playlistOptions)
            player?.load(playlistConfig)
            player?.playlist?.sources?.firstOrNull { it.config.metadata?.get("entryId") == this.entryIDToPlay }?.let { sourceToPlay ->
                player?.playlist?.seek(sourceToPlay, 0.0)
            }
        }
        currentVideoDetails = videoDetails
        player?.next(PlayerEvent.Ready::class.java) {
            _isPlayerReady.value = true
        }
        player?.next(PlayerEvent.Error::class.java) {
            Log.d("SDK", "Player error ${it.message}")
        }
        player?.next(PlayerEvent.Paused::class) {
            isPlayerPaused = player?.isPaused == true
        }
        player?.next(PlayerEvent.Play::class) {
            isPlayerPaused = player?.isPaused == true
        }
        if (autoplayEnabled && !isPlayerPaused) {
            player?.play()
        }
    }

    private fun createPlaylist(videoDetails: Array<PlaybackVideoDetails>): List<Source> {
        var sources = mutableListOf<Source>()
        for (details in videoDetails) {
            val videoSource = PlaybackSDKManager.createSource(details, authorizationToken = authorizationToken)
            videoSource?.let {
                sources.add(it)
            }
        }
        return sources
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
        } else if (autoplayEnabled && !isPlayerPaused) {
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

    fun clean() {
        // call this in main thread if it was called from background
        pauseVideo()
        currentVideoDetails = null
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
            // If the URL is incorrect, return false
            e.printStackTrace()
            false
        } catch (e: Exception) {
            // Handle any other unexpected exceptions
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
                if (parts.size == 2 && parts[0] != "ks") {
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