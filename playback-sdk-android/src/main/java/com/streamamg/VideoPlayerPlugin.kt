package com.streamamg

import android.view.View
import androidx.compose.runtime.Composable

interface VideoPlayerPlugin {
    val name: String
    val version: String

    fun setup()

    @Composable
    fun playerView(hlsUrl: String): Unit

    fun play()

    fun pause()

    fun removePlayer()
}