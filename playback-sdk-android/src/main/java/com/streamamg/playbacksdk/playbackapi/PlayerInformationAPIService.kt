import com.streamamg.PlayBackAPIError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal interface PlayerInformationAPI {
    suspend fun getPlayerInformation(): Flow<PlayerInformationResponseModel>
}

internal class PlayerInformationAPIService(private val apiKey: String) : PlayerInformationAPI {

    private val baseURL = "https://api.playback.streamamg.com/v1"
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun getPlayerInformation(): Flow<PlayerInformationResponseModel> {
        return flow {
            try {
                val url = URL("$baseURL/player")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("x-api-key", apiKey)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val playerInfo = json.decodeFromString<PlayerInformationResponseModel>(responseText)
                    emit(playerInfo)
                } else {
                    throw PlayBackAPIError.apiError(connection.responseCode, "Failed to get player information")
                }
            } catch (e: IOException) {
                throw PlayBackAPIError.NetworkError(e)
            }
        }.flowOn(Dispatchers.IO)
    }
}


@Serializable
internal data class PlayerInformationResponseModel(
    val player: PlayerInfo?,
    val defaults: Defaults?
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
    val playerName: String? = null,
    val envKey: String? = null
)

@Serializable
internal data class Resume(
    val enabled: Boolean?
)

@Serializable
internal data class Defaults(
    val player: String?
)
