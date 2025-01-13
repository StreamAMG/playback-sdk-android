package com.streamamg.playback_sdk_android_app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.streamamg.PlaybackSDKManager
import com.streamamg.PlaybackSDKManager.loadPlayer
import com.streamamg.player.plugin.VideoPlayerPluginManager
import com.streamamg.playback_sdk_android_app.ui.theme.PlaybacksdkandroidTheme
import com.streamamg.player.plugin.bitmovin.BitmovinPlayerPlugin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SDK
        val apiKey = "f3Beljhmlz2ea7M9TfErE6mKPsAcY3BrasMMEG24"
        PlaybackSDKManager.initialize(apiKey) { _, _ -> }

        // Register plugin
        //val customPlugin = NativeMediaPlayerPlugin()
        val customPlugin = BitmovinPlayerPlugin()
        VideoPlayerPluginManager.registerPlugin(customPlugin)

        val entryId = "0_qt9cy11s"
        val authorizationToken = "" // Can be null for free videos

        setContent {
            PlaybacksdkandroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(16f / 9f)
                                .fillMaxWidth()
                                .background(Color.Blue)
                        ) {
                            Log.d("PlaybackSDK", "Player loaded successfully.")
                            Text(text = "test")

                            // Load the player
                            loadPlayer(entryId, authorizationToken) { error ->
                                // Handle errors here
                                Log.e("PlaybackSDK", "Error occurred: $error")
                            }
                        }
                    }
                }
            }
        }
    }
}
