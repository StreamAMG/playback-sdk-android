package com.streamamg.api.player

import com.streamamg.PlayBackAPIError
import com.streamamg.PlayBackSDKManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json

/**
 * Represents the error response received from the playback API.
 */
data class ErrorResponse(val message: String)


/**
 * A service class responsible for handling playback API requests.
 */
internal class PlayBackAPIService(private val apiKey: String) : PlayBackAPI {

    // Class-level property to store the apiKey
    private lateinit var storedApiKey: String

    // Initialization block
    init {
        // Store the apiKey in the class-level property
        storedApiKey = apiKey

        // Additional initialization logic can go here if needed
    }

    /**
     * Retrieves video details for a given entry ID.
     *
     * @param entryId The unique identifier of the video entry.
     * @param authorizationToken Optional authorization token, can be null for free videos.
     * @return A Flow emitting the response model or an error.
     */
    override suspend fun getVideoDetails(entryId: String, authorizationToken: String?): Flow<PlaybackResponseModel> {
        return flow {
            val url = URL("${PlayBackSDKManager.baseURL}/entry/$entryId")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-api-key", apiKey)

            // JWT Token can be null for free videos.
            if (!authorizationToken.isNullOrEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $authorizationToken")
            }

            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseModel = Json.decodeFromString<PlaybackResponseModel>(responseText)
                    emit(responseModel)
                } else {
                    // TODO: handle error
                    val errorResponse = connection.errorStream?.let {
                        Json.decodeFromString<ErrorResponse>(it.reader().readText())
                    }
                    val errorMessage = errorResponse?.message ?: "Unknown authentication error"
                    throw PlayBackAPIError.apiError(connection.responseCode, errorMessage)
                }
            } catch (e: IOException) {
                throw PlayBackAPIError.NetworkError(e)
            } finally {
                connection.disconnect()
            }
        }
    }
}
