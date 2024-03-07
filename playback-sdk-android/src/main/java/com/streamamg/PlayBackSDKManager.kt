package com.streamamg

import PlayerInformationAPI
import PlayerInformationAPIService
import android.util.Log
import androidx.compose.runtime.Composable
import com.streamamg.api.player.PlayBackAPI
import com.streamamg.api.player.PlayBackAPIService
import com.streamamg.player.ui.PlaybackUIView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URL

object PlayBackSDKManager {
    private var playBackAPI: PlayBackAPI? = null
    private var playerInformationAPI: PlayerInformationAPI? = null
    private var bitmovinLicense: String? = null
    private var amgAPIKey: String? = null
    internal var baseURL = "https://api.playback.streamamg.com/v1"
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var playBackAPIService: PlayBackAPIService? = null

    fun initialize(
        apiKey: String,
        baseURL: String? = null,
        completion: (String?, Throwable?) -> Unit
    ) {
        if (apiKey.isEmpty()) {
            completion(null, SDKError.InitializationError)
            return
        }

        baseURL?.let { this.baseURL = it }
        amgAPIKey = apiKey
        playerInformationAPI = PlayerInformationAPIService(apiKey)
        playBackAPIService = PlayBackAPIService(apiKey)
        this.playBackAPI = playBackAPIService

        // Fetching Bitmovin license
        fetchPlayerInfo(completion)
    }

    fun fetchPlayerInfo(completion: (String?, Throwable?) -> Unit) {
        val playerInformationAPIExist = playerInformationAPI ?: run {
            completion(null, SDKError.InitializationError)
            return
        }
        coroutineScope.launch {
            try {
                val playerInfo = playerInformationAPIExist.getPlayerInformation().first()

                // Check if playerInfo is null before accessing its properties
                if (playerInfo?.player?.bitmovin?.license.isNullOrEmpty()) {
                    completion(null, SDKError.MissingLicense)
                    return@launch
                }

                // Extract the Bitmovin license
                val bitmovinLicense = playerInfo?.player?.bitmovin?.license

                // Set the received Bitmovin license
                this@PlayBackSDKManager.bitmovinLicense = bitmovinLicense

                // Log success message
                Log.d(
                    "PlayBackSDKManager",
                    "Bitmovin license fetched successfully: $bitmovinLicense"
                )

                // Call the completion handler with success
                completion(bitmovinLicense, null)
            } catch (e: Throwable) {
                Log.e("PlayBackSDKManager", "Error fetching Bitmovin license: $e")
                completion(null, e)
            }
        }
    }

    fun loadHLSStream(
        entryId: String,
        authorizationToken: String?,
        completion: (URL?, SDKError?) -> Unit
    ) {
        val playBackAPIExist = playBackAPI ?: run {
            completion(null, SDKError.InitializationError)
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val videoDetails =
                    playBackAPIExist.getVideoDetails(entryId, authorizationToken).first()

                // Log received video details
                Log.d("PlayBackSDKManager", "Received video details: $videoDetails")

                // Extract the HLS stream URL from video details
                val hlsURLString = videoDetails.media?.hls
                val hlsURL = hlsURLString?.let { URL(it) }

                if (hlsURL != null) {
                    // Call the completion handler with the HLS stream URL
                    completion(hlsURL, null)
                } else {
                    completion(null, SDKError.LoadHLSStreamError)
                }
            } catch (e: Throwable) {
                Log.e("PlayBackSDKManager", "Error loading HLS stream: $e")
                completion(null, SDKError.InitializationError) //TODO: add correct error
            }
        }
    }


    @Composable
    fun loadPlayer(
        entryID: String,
        authorizationToken: String,
        onError: ((PlayBackAPIError) -> Unit)?
    ): @Composable () -> Unit {

        val playbackUIView = PlaybackUIView(entryID, authorizationToken, onError)

        // Return a Composable function that renders the playback UI
        return {
            playbackUIView.Render()
        }

    }
}