package com.streamamg.api.player

import kotlinx.serialization.Serializable

/**
 * Data class representing the video details model for playback data.
 */

@Serializable
data class PlaybackVideoDetails(

    var videoId: String,
    var url: String?,
    var title: String?,
    var thumbnail: String?,
    var description: String?
)
