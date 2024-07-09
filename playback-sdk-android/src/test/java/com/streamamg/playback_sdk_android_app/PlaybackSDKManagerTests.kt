package com.streamamg.playback_sdk_android_app

import androidx.compose.runtime.Composable
import com.streamamg.PlaybackSDKManager
import com.streamamg.SDKError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.URL

class PlaybackSDKManagerTests {

    private lateinit var manager: PlaybackSDKManager
    private val apiKey = "EJEZPIezBkaf0EQ7ey5Iu2MDA2ARUkgc79eyDOnG"
    private val entryID = "0_qt9cy11s"

    @Before
    fun setUp() {
        manager = PlaybackSDKManager
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
    fun testInitializeWithCustomUserAgent() {
        val completion: (String?, SDKError?) -> Unit = { _, _ -> }
        runBlocking {
            manager.initialize(apiKey = apiKey, userAgent =  "userAgent") { license, error ->
                assertNotNull(license)
                assertNull(error)
            }
        }
    }

    @Test
    fun testLoadHLSStream() {
        val completion: (URL?, SDKError?) -> Unit = { _, _ -> }
        runBlocking {
            manager.loadHLSStream(entryID, null, null) { hlsURL, error ->
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
        val player = manager.loadPlayer(entryID, "authToken", ) {}
        assertNotNull(player)
    }
}
