package com.streamamg

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*

internal class PlaybackUIView(
    private val entryId: String,
    private val authorizationToken: String?,
    private val onError: ((PlayBackAPIError) -> Unit)?
) {
    private val pluginManager = VideoPlayerPluginManager
    private var hasFetchedVideoDetails by mutableStateOf(false)
    private var videoURL by mutableStateOf<String?>(null)

    @Composable
    internal fun Render() {
        LaunchedEffect(entryId) {
            loadHLSStream()
        }

        Box() {
            if (!hasFetchedVideoDetails) {
                // TODO: Add indicator
                // Show loading indicator
                // You can implement a loading indicator here
            } else {
                videoURL?.let { url ->
                    pluginManager.selectedPlugin?.let { plugin ->
                        plugin.playerView(url)
                    }
                } ?: run {
                    // TODO: Handle null video URL (Error UI View)
                }
            }
        }
    }

    private fun loadHLSStream() {
        PlayBackSDKManager.loadHLSStream(entryId, authorizationToken) { hlsURL, error ->
            if (error != null) {
                // Handle error
                onError?.invoke(PlayBackAPIError.InitializationError)
            } else {
                // Update video URL
                videoURL = hlsURL?.toString()
                hasFetchedVideoDetails = true
            }
        }
    }
}
