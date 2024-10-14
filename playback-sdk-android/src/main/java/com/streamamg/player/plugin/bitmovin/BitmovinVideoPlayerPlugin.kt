package com.streamamg.player.plugin.bitmovin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.ui.FullscreenHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.streamamg.player.plugin.VideoPlayerConfig
import com.streamamg.player.plugin.VideoPlayerPlugin


class BitmovinVideoPlayerPlugin : VideoPlayerPlugin, LifecycleCleaner {
    private var playerView: PlayerView? = null
    override val name: String = "Bitmovin"
    override val version: String = "1.0"

    private var hlsUrl: String = ""
    private var playerConfig = VideoPlayerConfig()
    private var playerBind: Player? = null
    private val fullscreen = mutableStateOf(false)
    private var playerViewModel: VideoPlayerViewModel? = null

    override fun setup(config: VideoPlayerConfig) {
        playerConfig.playbackConfig.autoplayEnabled = config.playbackConfig.autoplayEnabled
        playerConfig.playbackConfig.backgroundPlaybackEnabled = config.playbackConfig.backgroundPlaybackEnabled
        playerConfig.playbackConfig.fullscreenRotationEnabled = config.playbackConfig.fullscreenRotationEnabled
        playerConfig.playbackConfig.fullscreenEnabled = config.playbackConfig.fullscreenEnabled
    }

    @Composable
    override fun PlayerView(hlsUrl: String): Unit {
        val context = LocalContext.current
        val isJetpackCompose = when (context) {
            is ComponentActivity -> true
            else -> false
        }

        val activity = context.findActivity() as? ComponentActivity
        playerViewModel = if (isJetpackCompose) viewModel() else activity?.let {
            ViewModelProvider(it)[VideoPlayerViewModel::class.java]
        } ?: viewModel()

        this.hlsUrl = hlsUrl
        val currentLifecycle = LocalLifecycleOwner.current
        val lastHlsUrl = remember { mutableStateOf(hlsUrl) }
        val configuration = LocalConfiguration.current


        if (playerConfig.playbackConfig.backgroundPlaybackEnabled) {
            if (Build.VERSION.SDK_INT >= 33) {
                RequestMissingPermissions { granted ->
                    playerViewModel?.updatePermissionsState(granted, context)
                }
            } else {
                playerViewModel?.updatePermissionsState(true, context)
            }
        }

        DisposableEffect(hlsUrl) {
            playerViewModel?.initializePlayer(context, playerConfig, hlsUrl)
            playerBind = playerViewModel?.player
            onDispose {
                if (isJetpackCompose) {
                    playerViewModel?.unbindAndStopService(context)
                } else {
                    playerViewModel?.handleAppInBackground(context)
                }
            }
        }

        val isReady = playerViewModel?.isPlayerReady?.collectAsState()

        key(lastHlsUrl.value) {
            // Force recomposition when the HLS URL changes
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    playerView = PlayerView(context, playerViewModel?.player).apply {
                        keepScreenOn = true
                        player = playerViewModel?.player
                    }
                    playerView!!
                },
                update = { view ->
                    if (isReady?.value == true) {
                        view.player = playerViewModel?.player
                        if (playerConfig.playbackConfig.fullscreenEnabled)
                            playerView?.setFullscreenHandler(fullscreenHandler)
                        playerView?.invalidate()
                        playerViewModel?.updateBackgroundService(context)
                    }
                }
            )

            if (playerConfig.playbackConfig.fullscreenRotationEnabled)
                DetectRotationAndFullscreen(playerView)

            DisposableEffect(currentLifecycle, configuration) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            playerViewModel?.handleAppInBackground(context)
                        }
                        Lifecycle.Event.ON_START -> {
                            playerViewModel?.handleAppInForeground(context)
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            playerViewModel?.handleAppInBackground(context)
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            playerViewModel?.handleAppInForeground(context)
                        }
                        else -> {}
                    }
                }
                currentLifecycle.lifecycle.addObserver(observer)

                onDispose {
                    currentLifecycle.lifecycle.removeObserver(observer)
                }
            }
        }

        if (fullscreen.value) {
            LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            SystemBars(false)
        } else {
            LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
            SystemBars(true)
        }
    }

    @Composable
    fun LockScreenOrientation(orientation: Int) {
        val context = LocalContext.current
        DisposableEffect(orientation) {
            val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
            activity.requestedOrientation = orientation
            onDispose {}
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
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun RequestMissingPermissions(callback: ((Boolean) -> Unit)) {
        val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { granted ->
            callback(granted)
        }
        if (!permissionState.status.isGranted) {
            LaunchedEffect(
                key1 = Unit,
                block = { permissionState.launchPermissionRequest() }
            )
        } else {
           callback(true)
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
    }

    override fun clean(context: Context) {
        playerViewModel?.unbindAndStopService(context=context)
        playerViewModel?.clean()
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
        }

        override fun onFullscreenExitRequested() {
            fullscreen.value = false
        }
    }
}
