package com.streamamg.player.plugin.bitmovin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.ui.FullscreenHandler
import com.streamamg.PlaybackSDKManager
import com.streamamg.player.plugin.VideoPlayerPlugin


class BitmovinVideoPlayerPlugin : VideoPlayerPlugin {
    override val name: String = "Bitmovin"
    override val version: String = "1.0"

    private var playerViewLifecycleHandler: PlayerViewLifecycleHandler = PlayerViewLifecycleHandler()
    private lateinit var hlsUrl: String
    private lateinit var playerView: PlayerView
    private val fullscreen = mutableStateOf(false)

    override fun setup() {
        // TODO: Add here setup actions.
    }

    @Composable
    override fun PlayerView(hlsUrl: String): Unit {
        this.hlsUrl = hlsUrl
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val observers = remember { mutableListOf<DefaultLifecycleObserver>() }
        // Access context within the AndroidView composable
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->

                val playerConfig = PlayerConfig(key = PlaybackSDKManager.bitmovinLicense)
                val player = Player(context, playerConfig)
                playerView = PlayerView(context, player) // Use context provided here
                playerView.player?.load(SourceConfig.fromUrl(hlsUrl))

                val observer = object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) = playerViewLifecycleHandler.onStart(playerView)
                    override fun onResume(owner: LifecycleOwner) = playerViewLifecycleHandler.onResume(playerView)
                    override fun onPause(owner: LifecycleOwner) = playerViewLifecycleHandler.onPause(playerView)
                    override fun onStop(owner: LifecycleOwner) = playerViewLifecycleHandler.onStop(playerView)
                    override fun onDestroy(owner: LifecycleOwner) {
                        // Do not destroy the player in `onDestroy` as the player lifecycle is handled outside
                        // of the composable. This is achieved by setting the player to `null` before destroying.
                        playerView.player = null
                        playerViewLifecycleHandler.onDestroy(playerView)
                    }
                }

                lifecycle.addObserver(observer)
                observers.add(observer)

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
            playerView.player = null
        }
        ///playerView.player?.release
    }
}

// TODO: This might need to go in the VideoPlayerPlugin protocol.
private class PlayerViewLifecycleHandler {

    fun onStart(playerView: PlayerView) {
        playerView.player?.play() // Start playback when the composable starts
    }

    fun onResume(playerView: PlayerView) {
        playerView.player?.play() // Resume playback when the composable resumes
    }

    fun onPause(playerView: PlayerView) {
        playerView.player?.pause() // Pause playback when the composable is paused
    }

    fun onStop(playerView: PlayerView) {
        playerView.player?.pause() // Pause playback when the composable stops
    }

    @SuppressWarnings
    fun onDestroy(@Suppress("UNUSED_PARAMETER") playerView: PlayerView) {
        // Do nothing here as the player lifecycle is managed outside the composable
    }
}
