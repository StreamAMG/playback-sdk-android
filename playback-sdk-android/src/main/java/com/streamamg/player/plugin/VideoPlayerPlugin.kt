package com.streamamg.player.plugin

import androidx.compose.runtime.Composable

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

    //endregion

    //region Public methods

    /**
     * Performs setup operations for the video player plugin.
     */
    fun setup()

    /**
     * Renders the player view for the video player plugin.
     * @param hlsUrl The HLS URL of the video to be played.
     */
    @Composable
    fun PlayerView(hlsUrl: String)

    /**
     * Starts playback of the video.
     */
    fun play()

    /**
     * Pauses playback of the video.
     */
    fun pause()

    /**
     * Removes the player instance and cleans up resources.
     */
    fun removePlayer()

    //endregion

}
