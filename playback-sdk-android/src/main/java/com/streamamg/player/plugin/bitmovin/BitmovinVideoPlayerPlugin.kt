package com.streamamg.player.plugin.bitmovin

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.IBinder
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.ui.FullscreenHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.streamamg.PlaybackSDKManager
import com.streamamg.data.AnalyticsData
import com.streamamg.player.plugin.VideoPlayerConfig
import com.streamamg.player.plugin.VideoPlayerPlugin
import com.streamamg.player.plugin.analytics.MuxAnalyticsManager
import com.streamamg.player.ui.BackgroundPlaybackService


class BitmovinVideoPlayerPlugin : VideoPlayerPlugin {
    override val name: String = "Bitmovin"
    override val version: String = "1.0"

    private var hlsUrl: String = ""
    private var playerView: PlayerView? = null
    private var playerConfig = VideoPlayerConfig()
    private var playerBind: Player? = null
    private val fullscreen = mutableStateOf(false)
    private var isServiceBound = false
    private var mAnalyticsData: AnalyticsData? = null

    override fun setup(config: VideoPlayerConfig) {
        playerConfig.playbackConfig.autoplayEnabled = config.playbackConfig.autoplayEnabled
        playerConfig.playbackConfig.backgroundPlaybackEnabled =
            config.playbackConfig.backgroundPlaybackEnabled
    }

    @Composable
    override fun PlayerView(
        hlsUrl: String,
        analyticsData: AnalyticsData
    ): Unit {
        this.mAnalyticsData = analyticsData
        this.hlsUrl = hlsUrl
        val currentLifecycle = LocalLifecycleOwner.current
        val observers = remember { mutableListOf<DefaultLifecycleObserver>() }
        val lastHlsUrl = remember { mutableStateOf(hlsUrl) }

        if (playerConfig.playbackConfig.backgroundPlaybackEnabled) {
            if (Build.VERSION.SDK_INT >= 33) {
                // Managing new permissions for the Background service notifications
                RequestMissingPermissions()
            } else {
                // Bind and start the Background service without permissions
                bindAndStartBackgroundService(LocalContext.current)
            }
        }

        key(lastHlsUrl.value) {
            // Force recomposition when the HLS URL changes
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    if (!playerConfig.playbackConfig.backgroundPlaybackEnabled) {
                        // Init the Player without the Background service
                        val playerConfig =
                            PlayerConfig(key = PlaybackSDKManager.bitmovinLicense)
                        playerBind = Player(context, playerConfig)
                        playerView = PlayerView(context, playerBind)
                        initializePlayer(lastHlsUrl.value)
                        trackAnalytics(
                            context,
                            analyticsData.videoTitle,
                            analyticsData.videoId,
                            analyticsData.viewerId
                        )

                    } else {
                        // Init the Player with the Background service later in the BackgroundPlaybackService
                        if (playerView == null) {
                            playerView = PlayerView(context, playerBind)
                        }
                    }

                    val observer = object : DefaultLifecycleObserver {
                        override fun onStart(owner: LifecycleOwner) {
                            if (!playerConfig.playbackConfig.backgroundPlaybackEnabled) {
                                if (playerConfig.playbackConfig.autoplayEnabled) {
                                    playerBind?.play()
                                }
                            }
                        }

                        override fun onResume(owner: LifecycleOwner) {
                            if (!playerConfig.playbackConfig.backgroundPlaybackEnabled) {
                                if (playerConfig.playbackConfig.autoplayEnabled) {
                                    playerBind?.play()
                                }
                            }
                        }

                        override fun onPause(owner: LifecycleOwner) {
                            if (!playerConfig.playbackConfig.backgroundPlaybackEnabled) {
                                playerBind?.pause()
                            }
                        }

                        override fun onStop(owner: LifecycleOwner) {
                            if (!playerConfig.playbackConfig.backgroundPlaybackEnabled) {
                                playerBind?.pause()
                            }
                        }

                        override fun onDestroy(owner: LifecycleOwner) {
                            if (playerConfig.playbackConfig.backgroundPlaybackEnabled) {
                                unbindAndStopBackgroundService(context)
                            }
                        }
                    }

                    // Remove previous observer
                    if (observers.isNotEmpty()) {
                        observers.forEach { currentLifecycle.lifecycle.removeObserver(it) }
                        observers.clear()
                    }
                    // Add new observer
                    currentLifecycle.lifecycle.addObserver(observer)
                    observers.add(observer)
                    playerView!! // Directly return the PlayerView
                },
                update = { view ->
                    // Update the PlayerView with the new HLS URL
                    if (lastHlsUrl.value != hlsUrl) {
                        observers.firstOrNull()?.onStop(currentLifecycle)
                        observers.firstOrNull()?.onDestroy(currentLifecycle)
                        lastHlsUrl.value = hlsUrl

                        if (view.parent != null) {
                            (view.parent as? ViewGroup)?.removeView(view)
                            playerView?.setFullscreenHandler(null)
                        }
                    }
                }
            )
        }

        if (fullscreen.value) {
            LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            SystemBars(false)
        } else {
            LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
            SystemBars(true)
        }
    }

    private fun trackAnalytics(
        context: Context,
        videoTitle: String,
        videoId: String?,
        viewerId: String?
    ) {
        if (videoId != null && viewerId != null) {
            PlaybackSDKManager.muxEnvKey?.let { envKey ->
                MuxAnalyticsManager.track(
                    context,
                    playerView!!,
                    envKey,
                    PlaybackSDKManager.muxPlayerName,
                    videoTitle,
                    videoId,
                    viewerId
                )
            }
        }
    }

    @Composable
    fun LockScreenOrientation(orientation: Int) {
        val context = LocalContext.current
        DisposableEffect(orientation) {
            val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
            val originalOrientation = activity.requestedOrientation
            activity.requestedOrientation = orientation
            onDispose {
                // restore original orientation when view disappears
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    @Composable
    fun SystemBars(show: Boolean) {

        val context = LocalContext.current
        val activity = context.findActivity() ?: return
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            if (show) {
                show(WindowInsetsCompat.Type.statusBars())
                show(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun RequestMissingPermissions() {
        val context = LocalContext.current
        val permissionState =
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { granted ->
                if (granted) {
                    bindAndStartBackgroundService(context)
                }
            }
        if (!permissionState.status.isGranted) {
            LaunchedEffect(
                key1 = Unit,
                block = { permissionState.launchPermissionRequest() }
            )
        } else {
            bindAndStartBackgroundService(context)
        }
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services: List<ActivityManager.RunningServiceInfo> =
            activityManager.getRunningServices(Int.MAX_VALUE)

        for (runningServiceInfo in services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClass.name)) {
                return true
            }
        }
        return false
    }

    private fun bindAndStartBackgroundService(context: Context) {
        val intent = Intent(context, BackgroundPlaybackService::class.java)

        try {
            if (isServiceRunning(context, BackgroundPlaybackService::class.java)) {
                context.unbindService(mConnection)
                context.stopService(intent)
            }
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unbindAndStopBackgroundService(context: Context) {
        if (!isServiceBound) return

        val intent = Intent(context, BackgroundPlaybackService::class.java)

        try {
            context.unbindService(mConnection)
            context.stopService(intent)
            isServiceBound = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to the Service, cast the IBinder and get the Player instance
            val binder = service as BackgroundPlaybackService.BackgroundBinder
            playerBind = binder.player
            if (playerView == null) {
                playerView = PlayerView(binder.getService(), playerBind)
            }

            playerView?.player = playerBind

            initializePlayer(this@BitmovinVideoPlayerPlugin.hlsUrl)
            isServiceBound = true
            mAnalyticsData?.let { data ->
                trackAnalytics(
                    binder.getService().baseContext,
                    data.videoTitle,
                    data.videoId,
                    data.viewerId
                )
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isServiceBound = false
        }
    }

    private fun initializePlayer(hlsUrl: String) {
        playerBind?.next(PlayerEvent.Ready::class.java, this::checkEvent)
//        playerBind?.next(SourceEvent.Loaded::class.java, this::checkEvent)

        playerBind?.load(SourceConfig.fromUrl(hlsUrl))
    }

    private fun checkEvent(event: Event) {
        if (event is PlayerEvent.Ready) {
            playerView?.setFullscreenHandler(fullscreenHandler)
            playerView?.keepScreenOn = true
            if (playerConfig.playbackConfig.autoplayEnabled) {
                playerBind?.play()
            }
        }
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    override fun play() {
        playerBind?.play()
    }

    override fun pause() {
        playerBind?.pause()
    }

    override fun removePlayer() {
        playerBind?.destroy()
        MuxAnalyticsManager.release()
    }

    private val fullscreenHandler = object : FullscreenHandler {
        override fun onFullscreenRequested() {
            fullscreen.value = true
        }

        override fun onPause() {
        }

        override fun onResume() {
        }

        override val isFullscreen: Boolean
            get() = fullscreen.value

        override fun onDestroy() {
            MuxAnalyticsManager.release()
        }

        override fun onFullscreenExitRequested() {
            fullscreen.value = false
        }
    }
}
