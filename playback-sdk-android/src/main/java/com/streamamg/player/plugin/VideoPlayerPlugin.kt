package com.streamamg.player.plugin

import androidx.compose.runtime.Composable
import com.bitmovin.player.api.PlayerConfig
import com.streamamg.api.player.PlaybackVideoDetails
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface defining the contract for a video player plugin.
 * Documentation:
 * - This interface `VideoPlayerPlugin` defines the contract for a video player plugin.
 * - It includes properties for the name and version of the plugin.
 * - The interface provides methods for setup, rendering player view, playback control (play and pause), and removal of the player instance.
 * - Implementations of this interface should provide implementations for each method to define the behavior of the video player plugin.
 *
 */
interface VideoPlayerPlugin {

    //region Properties

    /**
     * Name of the video player plugin.
     */
    val name: String

    /**
     * Version of the video player plugin.
     */
    val version: String

    val events: SharedFlow<Any>

    //endregion

    //region Public methods

    /**
     * Performs setup operations for the video player plugin.
     */
    fun setup(config: VideoPlayerConfig)

    /**
     * Renders the player view for the video player plugin.
     * @param videoDetails A list of video details to be played.
     * @param entryIDToPlay The entryID to play at the beginning.
     * @param authorizationToken Optional authorization token if required to fetch the video details.
     * @param analyticsViewerId User identifier to be tracked in analytics.
     */
    @Composable
    fun PlayerView(videoDetails: Array<PlaybackVideoDetails>, entryIDToPlay: String?, authorizationToken: String?, analyticsViewerId: String?)

    // Optional methods to update player configuration (Used for Bitmovin player only)
    fun updatePlayerConfig(newConfig: PlayerConfig?) {
    }

    fun getPlayerConfig(): PlayerConfig? {
        return null
    }

    /**
     * Starts playback of the video.
     */
    fun play()

    /**
     * Pauses playback of the video.
     */
    fun pause()

    /**
     * Play next video of the Playlist.
     */
    fun playNext()

    /**
     * Play previous video of the Playlist.
     */
    fun playPrevious()

    /**
     * Play last video of the Playlist.
     */
    fun playLast()

    /**
     * Play first video of the Playlist.
     */
    fun playFirst()

    fun seek(entryId: String, completion: (Boolean) -> Unit)

    fun activeEntryId(): String?

    /**
     * Removes the player instance and cleans up resources.
     */
    fun removePlayer()

    //endregion

}
