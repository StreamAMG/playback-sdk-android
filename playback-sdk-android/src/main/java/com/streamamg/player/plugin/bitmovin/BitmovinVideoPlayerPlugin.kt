package com.streamamg.player.plugin.bitmovin

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.ui.FullscreenHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.streamamg.player.plugin.VideoPlayerPlugin
import com.streamamg.player.ui.BackgroundPlaybackService


class BitmovinVideoPlayerPlugin : VideoPlayerPlugin {
    override val name: String = "Bitmovin"
    override val version: String = "1.0"

    private var playerViewLifecycleHandler: PlayerViewLifecycleHandler? = null
    private lateinit var hlsUrl: String
    private lateinit var playerView: PlayerView
    private var playerBind: Player? = null
    private var bound = false
    private val fullscreen = mutableStateOf(false)

    override fun setup() {
        // TODO: Add here setup actions.
    }

    @Composable
    override fun PlayerView(hlsUrl: String): Unit {
        this.hlsUrl = hlsUrl
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val observers = remember { mutableListOf<DefaultLifecycleObserver>() }

        if (Build.VERSION.SDK_INT >= 33) {
            RequestMissingPermissions()
        } else {
            // Bind and start the Background service without permissions
            backgroundService(true, LocalContext.current)
        }

        // Access context within the AndroidView composable
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->

                playerView = PlayerView(context, null as Player?)

                val observer = object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        playerViewLifecycleHandler?.onStart(playerView)
                    }
                    override fun onResume(owner: LifecycleOwner) {
                        playerViewLifecycleHandler?.onResume(playerView)
                    }
                    override fun onPause(owner: LifecycleOwner) {
                        playerViewLifecycleHandler?.onPause(playerView)
                    }
                    override fun onStop(owner: LifecycleOwner) {
                        playerViewLifecycleHandler?.onStop(playerView)
                    }
                    override fun onDestroy(owner: LifecycleOwner) {
                        // Stop and unbind the Background service and reset the Player reference
                        backgroundService(false, context)

                        bound = false
                        playerBind = null
                        playerView.player = null
                        playerViewLifecycleHandler?.onDestroy(playerView)
                    }
                }

                lifecycle.addObserver(observer)
                observers.add(observer)

                playerView // Directly return the PlayerView
            }
        )

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
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun RequestMissingPermissions() {
        val context = LocalContext.current
        val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { granted ->
            if (granted) {
                backgroundService(true, context)
            }
        }
        if (!permissionState.status.isGranted) {
            LaunchedEffect(
                key1 = Unit,
                block = { permissionState.launchPermissionRequest() }
            )
        } else {
            // Bind and start the Background service
            backgroundService(true, context)
        }
    }

    private fun backgroundService(start: Boolean, context: Context) {
        if (start) {
            val intent = Intent(context, BackgroundPlaybackService::class.java)
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            context.startService(intent)
        } else {
            if (bound) {
                context.stopService(Intent(context, BackgroundPlaybackService::class.java))
                context.unbindService(mConnection)
            }
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

            // Attach the Player as soon as we have a reference
            playerView.player = playerBind

            initializePlayer()

            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    private fun initializePlayer() {

        playerViewLifecycleHandler = PlayerViewLifecycleHandler()

        val fullscreenHandler = object : FullscreenHandler {
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
        playerView.setFullscreenHandler(fullscreenHandler)
        playerView.keepScreenOn = true

        if (playerBind?.source == null) {
            playerBind?.load(SourceConfig.fromUrl(hlsUrl))
        }

        playerView.player?.play()
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    override fun play() {
        if (this::playerView
                .isInitialized) {
            playerView.player?.play()
        }
    }

    override fun pause() {
        if (this::playerView
                .isInitialized) {
            playerView.player?.pause()
        }
    }

    override fun removePlayer() {
        if (this::playerView
                .isInitialized) {
            playerView.player?.destroy()
        }
    }
}

// TODO: This might need to go in the VideoPlayerPlugin protocol.
private class PlayerViewLifecycleHandler {

    fun onStart(playerView: PlayerView) {

    }

    fun onResume(playerView: PlayerView) {

    }

    fun onPause(playerView: PlayerView) {

    }

    fun onStop(playerView: PlayerView) {

    }

    @SuppressWarnings
    fun onDestroy(@Suppress("UNUSED_PARAMETER") playerView: PlayerView) {
        // Do nothing here as the player lifecycle is managed outside the composable
    }
}
