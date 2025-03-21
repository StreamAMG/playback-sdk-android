package com.streamamg.api.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing the response model for playback data.
 */
@Serializable
internal data class PlaybackResponseModel(
    val message: String? = null,
    val reason: String? = null,
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val thumbnail: String? = null, // Use String instead of URL directly
    val duration: String? = null,
    val media: Media? = null,
    val playFrom: Int? = null,
    val adverts: List<Advert>? = null,
    val coverImg: CoverImages? = null,
    var entryId: String? = null
) {
    @Serializable
    data class Media(
        val hls: String? = null,
        val mpegdash: String? = null,
        val applehttp: String? = null
    )

    @Serializable
    data class Advert(
        @SerialName("adType") val adType: String? = null,
        val id: String? = null,
        val position: String? = null,
        val persistent: Boolean? = null,
        val discardAfterPlayback: Boolean? = null,
        val url: String? = null, // Use String instead of URL directly
        val preloadOffset: Int? = null,
        val skippableAfter: Int? = null
    )

    @Serializable
    data class CoverImages(
        @SerialName("360") val _360: String? = null, // Use String instead of URL directly
        @SerialName("720") val _720: String? = null, // Use String instead of URL directly
        @SerialName("1080") val _1080: String? = null // Use String instead of URL directly
    )
}

internal fun PlaybackResponseModel.toVideoDetails(): PlaybackVideoDetails? {
    this.entryId?.let { entryId ->
        return PlaybackVideoDetails(
            videoId = entryId,
            url = this.media?.hls,
            title = this.name,
            thumbnail = this.coverImg?._360,
            description = this.description
        )
    }
    return null
}
