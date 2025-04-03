package com.streamamg.player.plugin

import kotlinx.serialization.Serializable

@Serializable
data class VideoPlayerConfig(
    var playbackConfig: PlaybackConfig
) {
    constructor() : this(PlaybackConfig())
}

@Serializable
data class PlaybackConfig(
    var autoplayEnabled: Boolean = true,
    var backgroundPlaybackEnabled: Boolean = true,
    var fullscreenRotationEnabled: Boolean = true,
    var fullscreenEnabled: Boolean = true,
    var skipBackForwardButton: Boolean = false
)
