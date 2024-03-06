package com.streamamg

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import com.streamamg.playback_sdk_android.R


class BitmovinVideoPlayerPlugin : VideoPlayerPlugin {
    override val name: String = "Bitmovin"
    override val version: String = "1.0"

    private var hlsUrl: String = ""

    override fun setup() {
        // You can perform any setup required for the Bitmovin player here
    }

    @Composable
    override fun playerView(hlsUrl: String): Unit {
                return Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    View.inflate(context, R.layout.player_view, null)
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    val playerView = view.findViewById<PlayerView>(R.id.playerView)
                    initPlayer(playerView, hlsUrl)
                }
            )
        }
    }

    override fun play() {
        // Implement play functionality for Bitmovin player if needed
    }

    override fun pause() {
        // Implement pause functionality for Bitmovin player if needed
    }

    override fun removePlayer() {
        // Implement player removal for Bitmovin player if needed
    }

    private fun initPlayer(playerView: PlayerView, sourceUrl: String) {
        val sourceConfig = SourceConfig(sourceUrl, SourceType.Hls)
        val source = Source(sourceConfig)
        playerView.player?.load(source)
    }
}


