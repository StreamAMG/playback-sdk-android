import com.streamamg.PlaybackAPIError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal interface PlayerInformationAPI {
    suspend fun getPlayerInformation(userAgent: String?): Flow<PlayerInformationResponseModel>
}

internal class PlayerInformationAPIService(private val apiKey: String) : PlayerInformationAPI {

    private val baseURL = "https://api.playback.streamamg.com/v1"
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun getPlayerInformation(userAgent: String?): Flow<PlayerInformationResponseModel> {
        return flow {
            try {
                val url = URL("$baseURL/player")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("x-api-key", apiKey)
                if (!userAgent.isNullOrBlank()) {
                    connection.setRequestProperty("user-agent", userAgent)
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val playerInfo = json.decodeFromString<PlayerInformationResponseModel>(responseText)
                    emit(playerInfo)
                } else {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val playerInfo = json.decodeFromString<PlayerInformationResponseModel>(responseText)
                    emit(playerInfo)

                    val errorMessage = when (connection.responseCode) {
                        HttpURLConnection.HTTP_FORBIDDEN -> playerInfo.message ?: "API Key not provided or not valid"
                        else -> playerInfo.message ?: "Failed to get player information"
                    }

                    throw PlaybackAPIError.apiError(connection.responseCode, errorMessage, playerInfo.message ?: "Reason not available in this context.")
                }
            } catch (e: IOException) {
                throw PlaybackAPIError.NetworkError(e)
            }
        }.flowOn(Dispatchers.IO)
    }
}


@Serializable
internal data class PlayerInformationResponseModel(
    val player: PlayerInfo?,
    val defaults: Defaults?,
    val message: String? = null,
    val reason: String? = null
)

@Serializable
internal data class PlayerInfo(
    val bitmovin: Bitmovin?
)

@Serializable
internal data class Bitmovin(
    val license: String?,
    val integrations: Integrations?
)

@Serializable
internal data class Integrations(
    val mux: Mux?,
    val resume: Resume?
)

@Serializable
internal data class Mux(
    @SerialName("player_name") val playerName: String? = null,
    @SerialName("env_key") val envKey: String? = null
)

@Serializable
internal data class Resume(
    val enabled: Boolean?
)

@Serializable
internal data class Defaults(
    val player: String?
)
