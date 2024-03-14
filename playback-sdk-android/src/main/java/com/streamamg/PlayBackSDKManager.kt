package com.streamamg

import PlayerInformationAPI
import PlayerInformationAPIService
import android.util.Log
import androidx.compose.runtime.Composable
import com.streamamg.api.player.PlayBackAPI
import com.streamamg.api.player.PlayBackAPIService
import com.streamamg.player.ui.PlaybackUIView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.net.URL

/**
 * Singleton object to manage playback SDK functionalities.
 */
object PlayBackSDKManager {

    //region Properties

    //region Private Properties

    private lateinit var playBackAPI: PlayBackAPI
    private lateinit var playerInformationAPI: PlayerInformationAPI

    private lateinit var amgAPIKey: String
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var playBackAPIService: PlayBackAPIService

    //endregion

    //region Internal Properties

    /**
     * Base URL for the playback API.
     */
    internal var baseURL = "https://api.playback.streamamg.com/v1"
    internal lateinit var bitmovinLicense: String
    //endregion

    //endregion

    //region Public methods

    //region Initialization

    /**
     * Initializes the playback SDK.
     * @param apiKey The API key for authentication.
     * @param baseURL The base URL for the playback API. Default is null.
     * @param completion Callback to be invoked upon completion of initialization.
     */
    fun initialize(
        apiKey: String,
        baseURL: String? = null,
        completion: (String?, SDKError?) -> Unit
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

        // Fetching player information
        fetchPlayerInfo(completion)
    }

    //endregion

    //region Load Player

    /**
     * Loads the player UI.
     * @param entryID The ID of the entry.
     * @param authorizationToken The authorization token.
     * @param onError Callback for handling errors. Default is null.
     * @return Composable function to render the player UI.
     */
    @Composable
    fun loadPlayer(
        entryID: String,
        authorizationToken: String,
        onError: ((PlayBackAPIError) -> Unit)?
    ): @Composable () -> Unit {
        val playbackUIView = PlaybackUIView(entryID, authorizationToken, onError)

        return {
            playbackUIView.Render()
        }
    }

    //endregion

    //endregion

    //region Internal methods

    //region Player Information

    /**
     * Fetches player information including Bitmovin license.
     * @param completion Callback to be invoked upon completion of fetching player information.
     */
    private fun fetchPlayerInfo(completion: (String?, SDKError?) -> Unit) {
        coroutineScope.launch {
            try {
                val playerInfo = playerInformationAPI.getPlayerInformation().firstOrNull()

                if (playerInfo?.player?.bitmovin?.license.isNullOrEmpty()) {
                    completion(null, SDKError.MissingLicense)
                    return@launch
                }

                val bitmovinLicense = playerInfo?.player?.bitmovin?.license

                this@PlayBackSDKManager.bitmovinLicense = bitmovinLicense ?: run {
                    completion(null, SDKError.MissingLicense)
                    return@launch
                }

                Log.d(
                    "PlayBackSDKManager",
                    "Bitmovin license fetched successfully: $bitmovinLicense"
                )

                completion(bitmovinLicense, null)
            } catch (e: Throwable) {
                Log.e("PlayBackSDKManager", "Error fetching Bitmovin license: $e")
                completion(null, SDKError.FetchBitmovinLicenseError)
            }
        }
    }

    //endregion

    //region HLS Stream

    /**
     * Loads the HLS stream.
     * @param entryId The ID of the entry.
     * @param authorizationToken The authorization token.
     * @param completion Callback to be invoked upon completion of loading the HLS stream.
     */
    fun loadHLSStream(
        entryId: String,
        authorizationToken: String?,
        completion: (URL?, SDKError?) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val videoDetails =
                    playBackAPI.getVideoDetails(entryId, authorizationToken).firstOrNull()

                val hlsURLString = videoDetails?.media?.hls
                val hlsURL = hlsURLString?.let { URL(it) }

                if (hlsURL != null) {
                    completion(hlsURL, null)
                } else {
                    completion(null, SDKError.LoadHLSStreamError)
                }
            } catch (e: Throwable) {
                Log.e("PlayBackSDKManager", "Error loading HLS stream: $e")
                completion(null, SDKError.LoadHLSStreamError)
            }
        }
    }

    //endregion

    //endregion

}
