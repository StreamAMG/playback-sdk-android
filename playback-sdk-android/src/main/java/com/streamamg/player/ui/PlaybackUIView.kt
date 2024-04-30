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
import com.streamamg.PlaybackAPIError
import com.streamamg.PlaybackSDKManager
import com.streamamg.player.plugin.VideoPlayerPluginManager

@Composable
internal fun PlaybackUIView(authorizationToken: String?, entryId: String, userAgent: String?, onError: ((PlaybackAPIError) -> Unit)?) {
    var hasFetchedVideoDetails by remember { mutableStateOf(false) }
    var videoURL: String? by remember { mutableStateOf(null) }

    LaunchedEffect(entryId) {
        PlaybackSDKManager.loadHLSStream(entryId, authorizationToken, userAgent) { hlsURL, error ->
            if (error != null) {
                // Handle error
                onError?.invoke(error)
            } else {
                // Update video URL
                videoURL = hlsURL?.toString()
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
            videoURL?.let { url ->
                VideoPlayerPluginManager.selectedPlugin?.let { plugin ->
                    plugin.PlayerView(url)
                }
            } ?: run {
                // TODO: Handle null video URL (Error UI View)
            }
        }
    }
}
