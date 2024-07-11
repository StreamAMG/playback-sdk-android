package com.streamamg.playback_sdk_android_app

import androidx.compose.runtime.Composable
import com.streamamg.PlaybackSDKManager
import com.streamamg.SDKError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.URL

class PlaybackSDKManagerTests {

    private lateinit var manager: PlaybackSDKManager
    private val apiKey = "EJEZPIezBkaf0EQ7ey5Iu2MDA2ARUkgc79eyDOnG"
    private val entryID = "0_qt9cy11s"

    val scope = TestScope()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scope.testScheduler))
        manager = PlaybackSDKManager
    }

    @Test
    fun testInitialization() {
        assertNotNull(manager)
    }

    @Test
    fun testInitializeWithValidAPIKey() = runTest {
        manager.initialize(apiKey) { license, error ->
            assertNotNull(license)
            assertNull(error)
        }
    }

    @Test
    fun testInitializeWithCustomUserAgent() = runTest {
        manager.initialize(apiKey = apiKey, userAgent =  "userAgent") { license, error ->
            assertNotNull(license)
            assertNull(error)
        }
    }

    @Test
    fun testLoadHLSStream() = runTest {
        manager.loadHLSStream(entryID, null, null) { hlsURL, error ->
            assertNotNull(hlsURL)
            assertNull(error)
        }
    }

    @Test
    fun testInitializeWithEmptyAPIKey() = runTest {
        manager.initialize("", userAgent =  "userAgent") { _, error ->
            assertNull(error)
        }
    }

//    @Composable
//    @Test
//    fun testLoadPlayer() {
//        val player = manager.loadPlayer(entryID, "authToken", ) {}
//        assertNotNull(player)
//    }
}
