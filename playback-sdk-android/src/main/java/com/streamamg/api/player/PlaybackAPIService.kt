package com.streamamg.api.player

import com.streamamg.PlaybackAPIError
import com.streamamg.PlaybackSDKManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Represents the error response received from the playback API.
 */
data class ErrorResponse(val message: String)


/**
 * A service class responsible for handling playback API requests.
 */
internal class PlaybackAPIService(private val apiKey: String) : PlaybackAPI {

    // Class-level property to store the apiKey
    private var storedApiKey: String = apiKey

    // Initialization block
    init {
        // Store the apiKey in the class-level property

        // Additional initialization logic can go here if needed
    }

    /**
     * Retrieves video details for a given entry ID.
     *
     * @param entryId The unique identifier of the video entry.
     * @param authorizationToken Optional authorization token, can be null for free videos.
     * @return A Flow emitting the response model or an error.
     */
    override suspend fun getVideoDetails(
        entryId: String,
        authorizationToken: String?,
        userAgent: String?
    ): Flow<PlaybackResponseModel> = flow {
        val url = URL("${PlaybackSDKManager.baseURL}/entry/$entryId")
        (url.openConnection() as? HttpURLConnection)?.run {
            try {
                setRequestProperty("Accept", "application/json")
                setRequestProperty("x-api-key", apiKey)
                if (!authorizationToken.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $authorizationToken")
                }

                if (!userAgent.isNullOrBlank()) {
                    setRequestProperty("user-agent", userAgent)
                }

                val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    errorStream.bufferedReader().use(BufferedReader::readText)
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val json = Json { ignoreUnknownKeys = true }
                    val responseModel = json.decodeFromString<PlaybackResponseModel>(responseText)
                    responseModel.entryId = entryId
                    emit(responseModel)
                } else {
                    val json = Json { ignoreUnknownKeys = true }
                    val errorResponse = json.decodeFromString<PlaybackResponseModel>(responseText)

                    val errorMessage = errorResponse.message ?: when (responseCode) {
                        HttpURLConnection.HTTP_FORBIDDEN -> "API Key not provided or not valid"
                        else -> "Failed to get player information"
                    }

                    throw PlaybackAPIError.apiError(
                        responseCode,
                        errorMessage,
                        errorResponse.message ?: "Reason not available in this context."
                    )
                }
            } catch (e: IOException) {
                throw PlaybackAPIError.NetworkError(e)
            } finally {
                disconnect()
            }
        } ?: throw PlaybackAPIError.ApiError(
            0,
            "Unable to open connection",
            "No connection established"
        )
    }
}
