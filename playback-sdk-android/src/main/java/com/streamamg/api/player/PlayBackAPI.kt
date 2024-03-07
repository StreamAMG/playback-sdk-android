package com.streamamg.api.player

import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the methods required to interact with the Playback API.
 */
internal interface PlayBackAPI {

    /**
     * Retrieves video details for a given entry ID.
     *
     * @param entryId The unique identifier of the video entry.
     * @param authorizationToken Optional authorization token, can be null for free videos.
     * @return A Flow emitting the response model or an error.
     */
    suspend fun getVideoDetails(entryId: String, authorizationToken: String?): Flow<PlaybackResponseModel>
}
