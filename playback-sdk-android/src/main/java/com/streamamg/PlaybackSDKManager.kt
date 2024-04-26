package com.streamamg

import PlayerInformationAPI
import PlayerInformationAPIService
import android.util.Log
import androidx.compose.runtime.Composable
import com.streamamg.api.player.PlaybackAPI
import com.streamamg.api.player.PlaybackAPIService
import com.streamamg.player.ui.PlaybackUIView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.net.URL

/**
 * Singleton object to manage playback SDK functionalities.
 */
object PlaybackSDKManager {

    //region Properties

    //region Private Properties

    private lateinit var playBackAPI: PlaybackAPI
    private lateinit var playerInformationAPI: PlayerInformationAPI

    private lateinit var amgAPIKey: String
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var playBackAPIService: PlaybackAPIService

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
        playBackAPIService = PlaybackAPIService(apiKey)
        this.playBackAPI = playBackAPIService

        // Fetching player information
        fetchPlayerInfo(completion)
    }

    //endregion

    //region Load Player

    /**
     * Composable function that loads and renders the player UI.
     * @param entryID The ID of the entry.
     * @param authorizationToken The authorization token.
     * @param userAgent Custom user-agent header for the loading request. Default is Android system http user agent.
     * @param onError Callback for handling errors. Default is null.
     */
    @Composable
    fun loadPlayer(
        entryID: String,
        authorizationToken: String?,
        userAgent: String? = System.getProperty("http.agent"),
        onError: ((PlaybackAPIError) -> Unit)?
    ) {
        PlaybackUIView(
            authorizationToken = authorizationToken,
            entryId = entryID,
            userAgent = userAgent,
            onError = onError
        )
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

                this@PlaybackSDKManager.bitmovinLicense = bitmovinLicense ?: run {
                    completion(null, SDKError.MissingLicense)
                    return@launch
                }

                completion(bitmovinLicense, null)
            } catch (e: Throwable) {
                Log.e("PlaybackSDKManager", "Error fetching Bitmovin license: $e")
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
        userAgent: String?,
        completion: (URL?, PlaybackAPIError?) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            playBackAPI.getVideoDetails(entryId, authorizationToken, userAgent)
                .catch { e ->
                    // Handle the PlaybackAPIError or any other Throwable as a PlaybackAPIError
                    when (e) {
                        is PlaybackAPIError -> completion(null, e)
                        else -> completion(null, PlaybackAPIError.NetworkError(e))
                    }
                }
                .collect { videoDetails ->
                    // Successfully retrieved video details, now check for the HLS URL
                    val hlsURLString = videoDetails.media?.hls
                    if (!hlsURLString.isNullOrEmpty()) {
                        val hlsURL = URL(hlsURLString)
                        completion(hlsURL, null)
                    } else {
                        // No HLS URL found in the response
                        completion(null, PlaybackAPIError.ApiError(0, "HLS URL not available", "No HLS URL found in the response"))
                    }
                }
        }
    }




    //endregion


}
