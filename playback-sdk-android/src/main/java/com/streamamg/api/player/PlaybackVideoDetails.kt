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
    var description: String?,
    // Resume position provided by the playback API (unit is assumed to match Bitmovin seek seconds).
    // Only used when the backend RESUME flag enables it.
    var playFrom: Int? = null
)
