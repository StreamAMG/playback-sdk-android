package com.streamamg.playback_sdk_android_app

import androidx.compose.runtime.Composable
import com.streamamg.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URL

class PlayBackSDKManagerTests {

    private lateinit var manager: PlayBackSDKManager
    private val apiKey = "f3Beljhmlz2ea7M9TfErE6mKPsAcY3BrasMMEG24"
    private val entryID = "0_k3mz0mf8"

    @Before
    fun setUp() {
        manager = PlayBackSDKManager
    }

    @Test
    fun testInitialization() {
        assertNotNull(manager)
    }

    @Test
    fun testInitializeWithValidAPIKey() {
        val completion: (String?, SDKError?) -> Unit = { _, _ -> }
        runBlocking {
            manager.initialize(apiKey) { license, error ->
                assertNotNull(license)
                assertNull(error)
            }
        }
    }

    @Test
    fun testLoadHLSStream() {
        val completion: (URL?, SDKError?) -> Unit = { _, _ -> }
        runBlocking {
            manager.loadHLSStream(entryID, null) { hlsURL, error ->
                assertNotNull(hlsURL)
                assertNull(error)
            }
        }
    }

    @Test
    fun testInitializeWithEmptyAPIKey() {
        val completion: (String?, SDKError?) -> Unit = { _, _ -> }
        runBlocking {
            manager.initialize("") { _, error ->
                assertNull(error)
            }
        }
    }

    @Composable
    @Test
    fun testLoadPlayer() {
        val player = manager.loadPlayer(entryID, "authToken", null)
        assertNotNull(player)
    }
}
