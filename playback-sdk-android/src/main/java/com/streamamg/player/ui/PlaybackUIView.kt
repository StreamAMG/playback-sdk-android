package com.streamamg.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.streamamg.PlaybackAPIError
import com.streamamg.PlaybackSDKManager
import com.streamamg.api.player.PlaybackVideoDetails
import com.streamamg.api.player.toVideoDetails
import com.streamamg.player.plugin.VideoPlayerPluginManager
import com.streamamg.player.plugin.bitmovin.LifecycleCleaner

object PlaybackUIView {

    @Composable
    fun PlaybackUIView(
        authorizationToken: String?,
        entryId: String,
        analyticsViewerId: String?,
        onError: ((PlaybackAPIError) -> Unit)?
    ) {
        var hasFetchedVideoDetails by remember { mutableStateOf(false) }
        var videoDetails: PlaybackVideoDetails? by remember { mutableStateOf(null) }
        val context = LocalContext.current

        LaunchedEffect(entryId) {
            PlaybackSDKManager.loadHLSStream(entryId, authorizationToken) { details, error ->
                if (error != null) {
                    // Handle error
                    VideoPlayerPluginManager.selectedPlugin?.let { plugin ->
                        (plugin as? LifecycleCleaner)?.clean(context)
                    }
                    onError?.invoke(error)
                } else {
                    // Update video URL
                    videoDetails = details?.toVideoDetails()
                    hasFetchedVideoDetails = true
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasFetchedVideoDetails) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                videoDetails?.let { details ->
                    VideoPlayerPluginManager.selectedPlugin?.let { plugin ->
                        plugin.PlayerView(arrayOf(details), entryId, authorizationToken, analyticsViewerId)
                    }
                } ?: run {
                    // TODO: Handle null video URL (Error UI View)
                }
            }
        }
    }

    @Composable
    fun PlaybackUIView(
        entryIDs: Array<String>,
        entryIDToPlay: String?,
        authorizationToken: String?,
        analyticsViewerId: String?,
        onErrors: ((Array<PlaybackAPIError>) -> Unit)?
    ) {
        var hasFetchedVideoDetails by remember { mutableStateOf(false) }
        var videoDetails: Array<PlaybackVideoDetails> by remember { mutableStateOf(emptyArray()) }
        val context = LocalContext.current

        LaunchedEffect(entryIDToPlay) {
            PlaybackSDKManager.loadAllHLSStream(entryIDs, authorizationToken) { details, error ->

                if (error != null) {
                    // Handle error
                    VideoPlayerPluginManager.selectedPlugin?.let { plugin ->
                        (plugin as? LifecycleCleaner)?.clean(context)
                    }
                    onErrors?.invoke(arrayOf(error))
                } else {
                    if (details?.first?.isEmpty() == false) {
                        for (detail in details.first!!) {
                            detail.toVideoDetails()?.let { videoDetail ->
                                videoDetails += videoDetail
                            }
                        }
                        hasFetchedVideoDetails = true
                        if (details.second?.isNotEmpty() == true) {
                            onErrors?.invoke(details.second!!)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasFetchedVideoDetails) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                if (videoDetails.isNotEmpty()) {
                    VideoPlayerPluginManager.selectedPlugin?.let { plugin ->
                        plugin.PlayerView(videoDetails, entryIDToPlay, authorizationToken, analyticsViewerId)
                    }
                }
            }
        }
    }
}