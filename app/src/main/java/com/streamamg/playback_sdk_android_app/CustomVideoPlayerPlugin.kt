package com.streamamg.playback_sdk_android_app

import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.streamamg.player.plugin.VideoPlayerPlugin

class NativeMediaPlayerPlugin : VideoPlayerPlugin {
    private var mediaPlayer: MediaPlayer? = null

    override val name: String
        get() = "Native Media Player Plugin"
    override val version: String
        get() = "1.0"

    override fun setup() {
        mediaPlayer = MediaPlayer()
    }

    @Composable
    override fun playerView(hlsUrl: String): Unit {
        setup()
        val textureView = rememberTextureView()

        mediaPlayer?.apply {
            setDataSource(hlsUrl)
            setOnPreparedListener { mp ->
                // When MediaPlayer is prepared, set the surface texture
                textureView.surfaceTexture?.let {
                    mp.setSurface(Surface(it))
                    mp.start()
                }
            }
            prepareAsync()
        }

        AndroidView(factory = { textureView })
    }

    override fun play() {
        mediaPlayer?.start()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun removePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
private fun rememberTextureView(): TextureView {
    val context = LocalContext.current
    val textureView = remember {
        TextureView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Use LaunchedEffect to ensure the surface texture is available before usage
    LaunchedEffect(textureView) {
        textureView.awaitSurfaceTexture()?.let {
            // Surface texture is available, trigger recomposition
            textureView.invalidate()
        }
    }

    return textureView
}

private suspend fun TextureView.awaitSurfaceTexture(): Surface? {
    while (surfaceTexture == null) {
        // Wait for surface texture to be available
    }
    return Surface(surfaceTexture)
}
