package com.streamamg.playback_sdk_android_app

import com.streamamg.PlaybackSDKManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

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
        manager.initialize(apiKey, userAgent =  "userAgent") { license, error ->
            assertNotNull(license)
            assertNull(error)
        }
    }

    @Test
    fun testLoadHLSStream() = runTest {
        manager.initialize(apiKey = apiKey, userAgent =  "userAgent") { license, error ->
            assertNotNull(license)
            assertNull(error)
        }
        manager.loadHLSStream(entryID, null, "userAgent") { hlsURL, title, error ->
            assertNotNull(hlsURL)
            assertNull(error)
        }
    }

    @Test
    fun testInitializeWithEmptyAPIKey() = runTest {
        manager.initialize("", userAgent =  "userAgent") { _, error ->
            assertNotNull(error) // Expect an error with empty API key
        }
    }
}
