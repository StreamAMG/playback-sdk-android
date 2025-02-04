package com.streamamg

import PlayerInformationAPI
import PlayerInformationAPIService
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.player.api.analytics.AnalyticsSourceConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.casting.BitmovinCastManager
import com.streamamg.api.player.PlaybackAPI
import com.streamamg.api.player.PlaybackAPIService
import com.streamamg.api.player.PlaybackResponseModel
import com.streamamg.api.player.PlaybackVideoDetails
import com.streamamg.playback_sdk_android.BuildConfig
import com.streamamg.player.ui.PlaybackUIView.PlaybackUIView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Singleton object to manage playback SDK functionalities.
 */
object PlaybackSDKManager {

    //region Properties

    //region Private Properties

    private var playbackAPI: PlaybackAPI? = null
    private lateinit var playerInformationAPI: PlayerInformationAPI

    private lateinit var amgAPIKey: String
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var playbackAPIService: PlaybackAPIService

    //endregion

    //region Internal Properties

    /**
     * Base URL for the playback API.
     */
    internal var baseURL = "https://api.playback.streamamg.com/v1"
    internal var bitmovinLicense: String = ""
    internal var analyticsLicense: String? = null
    private var userAgent: String? = null

    val playbackSdkVersion = BuildConfig.SDK_VERSION

    //endregion

    //endregion

    //region Public methods

    //region Initialization

    /**
     * Initializes the playback SDK.
     * @param apiKey The API key for authentication.
     * @param baseURL The base URL for the playback API. Default is null.
     * @param userAgent Custom user-agent header for the loading requests. Default is Android system http user agent.
     * @param completion Callback to be invoked upon completion of initialization.
     */
    fun initialize(
        apiKey: String,
        baseURL: String? = null,
        userAgent: String? = System.getProperty("http.agent"),
        completion: (String?, SDKError?) -> Unit
    ) {
        if (apiKey.isEmpty()) {
            completion(null, SDKError.InitializationError)
            return
        }

        BitmovinCastManager.initialize()

        baseURL?.let { this.baseURL = it }
        amgAPIKey = apiKey
        playerInformationAPI = PlayerInformationAPIService(apiKey)
        playbackAPIService = PlaybackAPIService(apiKey)
        this.playbackAPI = playbackAPIService
        this.userAgent = userAgent

        // Fetching player information
        fetchPlayerInfo(userAgent, completion)
    }

    //endregion

    //region Load Player

    /**
     * Composable function that loads and renders the player UI.
     * @param entryID The ID of the entry.
     * @param authorizationToken The authorization token.
     * @param analyticsViewerId The user's id to be tracked in analytics
     * @param onError Callback for handling errors. Default is null.
     */
    @Composable
    fun loadPlayer(
        entryID: String,
        authorizationToken: String?,
        analyticsViewerId: String? = null,
        onError: ((PlaybackAPIError) -> Unit)?
    ) {
        PlaybackUIView(
            authorizationToken = authorizationToken,
            entryId = entryID,
            analyticsViewerId = analyticsViewerId,
            onError = onError
        )
    }

    //endregion

    //region Load Playlist

    /**
     * Composable function that loads a list of videos and renders the player UI.
     * @param entryIDs A list of the videos to be loaded.
     * @param entryIDToPlay The first video Id to be played. If not provided, the first video in the entryIDs array will be played.
     * @param authorizationToken The authorization token.
     * @param analyticsViewerId The user's id to be tracked in analytics
     * @param onErrors Return a list of potential playback errors that may occur during the loading process for single entryId.
     */
    @Composable
    fun loadPlaylist(
        entryIDs: Array<String>,
        entryIDToPlay: String? = null,
        authorizationToken: String? = null,
        analyticsViewerId: String? = null,
        onErrors: ((Array<PlaybackAPIError>) -> Unit)?
    ) {
        PlaybackUIView(
            entryIDs = entryIDs,
            entryIDToPlay = entryIDToPlay,
            authorizationToken = authorizationToken,
            analyticsViewerId = analyticsViewerId,
            onErrors = onErrors
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
    private fun fetchPlayerInfo(userAgent: String?, completion: (String?, SDKError?) -> Unit) {
        coroutineScope.launch {
            try {
                val playerInfo = playerInformationAPI.getPlayerInformation(userAgent).firstOrNull()

                if (playerInfo?.player?.bitmovin?.license.isNullOrEmpty()) {
                    completion(null, SDKError.MissingLicense)
                    return@launch
                }

                val bitmovinLicense = playerInfo?.player?.bitmovin?.license
                val analyticsLicense = playerInfo?.player?.bitmovin?.integrations?.mux?.envKey

                this@PlaybackSDKManager.analyticsLicense = analyticsLicense

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
     * @param userAgent Custom user-agent header for the loading requests.
     * @param completion Callback to be invoked upon completion of loading the HLS stream.
     */
    internal fun loadHLSStream(
        entryId: String,
        authorizationToken: String?,
        completion: (PlaybackResponseModel?, PlaybackAPIError?) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            playbackAPI?.getVideoDetails(entryId, authorizationToken, userAgent)
                ?.catch { e ->
                    // Handle the PlaybackAPIError or any other Throwable as a PlaybackAPIError
                    when (e) {
                        is PlaybackAPIError -> completion(null, e)
                        else -> completion(null, PlaybackAPIError.NetworkError(e))
                    }
                }
                ?.collect { videoDetails ->
                    if (videoDetails != null) {
                        // Successfully retrieved video details
                        completion(videoDetails, null)
                    } else {
                        // No video details found in the response
                        completion(null, PlaybackAPIError.ApiError(0, "Video details not available", "No Video details found in the response"))
                    }
                }
        }
    }

    //endregion

    //region All HLS Stream

    /**
     * Loads All the HLS stream.
     * @param entryIDs The ID list of the entries.
     * @param authorizationToken The authorization token.
     * @param userAgent Custom user-agent header for the loading requests.
     * @param completion Callback to be invoked upon completion of loading the HLS stream.
     */
    internal fun loadAllHLSStream(
        entryIDs: Array<String>,
        authorizationToken: String?,
        completion: (Pair<Array<PlaybackResponseModel>?, Array<PlaybackAPIError>?>?, PlaybackAPIError?) -> Unit
    ) {
        var videoDetails: ArrayList<PlaybackResponseModel> = ArrayList()
        var playbackErrors: ArrayList<PlaybackAPIError> = ArrayList()

        if (playbackAPI == null) {
            completion(null, PlaybackAPIError.InitializationError)
            return
        }

        coroutineScope.launch(Dispatchers.IO) {

            val deferredList = entryIDs.map { entryId ->
                async {
                    playbackAPI?.getVideoDetails(entryId, authorizationToken, userAgent)
                        ?.catch { e ->
                            // Handle the PlaybackAPIError or any other Throwable as a PlaybackAPIError
                            when (e) {
                                is PlaybackAPIError -> playbackErrors.add(e)
                                else -> playbackErrors.add(PlaybackAPIError.NetworkError(e))
                            }
                        }
                        ?.collect { videoDetail ->
                            if (videoDetail != null) {
                                // Successfully retrieved video details
                                videoDetails.add(videoDetail)
                            } else {
                                // No video details found in the response
                                playbackErrors.add(PlaybackAPIError.ApiError(0, "Video details not available", "No Video details found in the response"))
                            }
                        }
                }
            }

            // Wait for all deferred results
            deferredList.awaitAll()

            val orderedVideoDetails = entryIDs.mapNotNull { entryId ->
                videoDetails.find { it.entryId == entryId }
            }

            // Call completion after all getVideoDetails calls are completed
            completion(Pair(orderedVideoDetails.toTypedArray(), playbackErrors.toTypedArray()), null)
        }
    }

    //endregion

    // region Create Source

    fun createSource(details: PlaybackVideoDetails, authorizationToken: String?): Source? {
        if (details.url.isNullOrEmpty()) return null

        val sourceConfig = SourceConfig.fromUrl(details.url!!)

        val metadata = mutableMapOf<String, String>()

        if (details.videoId.isNotEmpty()) {
            metadata["entryId"] = details.videoId
        }

        // Adding extra details
        metadata["details"] = details.toString()
        authorizationToken?.let {
            metadata["authorizationToken"] = it
        }
        sourceConfig.metadata = metadata

        val sourceMetadata = SourceMetadata(
            title = details.title,
            videoId = details.videoId,
            customData = CustomData(
                customData1 = details.description
            )
        )

        return Source(sourceConfig, AnalyticsSourceConfig.Enabled(sourceMetadata))
    }

    //endregion

    //region Chromecast

    /**
     * Each Activity that uses Cast related API's has to call the following function before using any cast related API.
     * Update Chromecast context.
     * @param context The context of the Activity.
     */
    fun updateCastContext(context: Context) {
        BitmovinCastManager.getInstance().updateContext(context)
    }

    //endregion
}
