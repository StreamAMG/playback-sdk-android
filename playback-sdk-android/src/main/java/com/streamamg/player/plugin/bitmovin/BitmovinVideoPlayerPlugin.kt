package com.streamamg.player.plugin.bitmovin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.source.SourceConfig
import com.streamamg.PlaybackSDKManager
import com.streamamg.player.plugin.VideoPlayerPlugin


class BitmovinVideoPlayerPlugin : VideoPlayerPlugin {
    override val name: String = "Bitmovin"
    override val version: String = "1.0"

    private var playerViewLifecycleHandler: PlayerViewLifecycleHandler = PlayerViewLifecycleHandler()
    private lateinit var hlsUrl: String
    private lateinit var playerView: PlayerView

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

                playerView // Directly return the PlayerView
            }
        )
    }


    override fun play() {
        playerView.player?.play()
    }

    override fun pause() {
        playerView.player?.pause()
    }

    override fun removePlayer() {
        playerView.player = null
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

    fun onDestroy(playerView: PlayerView) {
        // Do nothing here as the player lifecycle is managed outside the composable
    }
}
