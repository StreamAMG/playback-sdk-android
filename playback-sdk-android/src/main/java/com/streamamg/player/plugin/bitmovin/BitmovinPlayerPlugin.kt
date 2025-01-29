package com.streamamg.player.plugin.bitmovin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.ui.FullscreenHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.streamamg.PlaybackSDKManager
import com.streamamg.api.player.PlaybackVideoDetails
import com.streamamg.api.player.toVideoDetails
import com.streamamg.player.plugin.VideoPlayerConfig
import com.streamamg.player.plugin.VideoPlayerPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose


internal interface FullscreenListener {
    fun enterFullscreen()
    fun exitFullscreen()
}

class BitmovinPlayerPlugin : VideoPlayerPlugin, LifecycleCleaner {
    private var playerView: PlayerView? = null
    override val name: String = "Bitmovin"
    override val version: String = "1.0"
    private val _eventFlow = MutableSharedFlow<Any>()
    override val events = _eventFlow.asSharedFlow()
    private var isListenerActive = true

    private var videoDetails: Array<PlaybackVideoDetails>? = null
    private var analyticsViewerId: String? = null
    private var playerConfig = VideoPlayerConfig()
    private var playerBind: Player? = null
        set(value) {
            field = value
            if (value != null) {
                listenToPlayerEvents()
            }
        }
    private val fullscreen = mutableStateOf(false)
    private var playerViewModel: VideoPlayerViewModel? = null

    override fun setup(config: VideoPlayerConfig) {
        playerConfig.playbackConfig.autoplayEnabled = config.playbackConfig.autoplayEnabled
        playerConfig.playbackConfig.backgroundPlaybackEnabled = config.playbackConfig.backgroundPlaybackEnabled
        playerConfig.playbackConfig.fullscreenRotationEnabled = config.playbackConfig.fullscreenRotationEnabled
        playerConfig.playbackConfig.fullscreenEnabled = config.playbackConfig.fullscreenEnabled
    }

    @Composable
    override fun PlayerView(videoDetails: Array<PlaybackVideoDetails>,
                            entryIDToPlay: String?,
                            authorizationToken: String?,
                            analyticsViewerId: String?) {
        val context = LocalContext.current
        val isJetpackCompose = when (context) {
            is ComponentActivity -> true
            else -> false
        }

        val activity = context.findActivity() as? ComponentActivity
        playerViewModel = if (isJetpackCompose) viewModel() else activity?.let {
            ViewModelProvider(it)[VideoPlayerViewModel::class.java]
        } ?: viewModel()

        this.analyticsViewerId = analyticsViewerId
        this.videoDetails = videoDetails
        val currentLifecycle = LocalLifecycleOwner.current
        val configuration = LocalConfiguration.current


        if (playerConfig.playbackConfig.backgroundPlaybackEnabled) {
            if (Build.VERSION.SDK_INT >= 33) {
                RequestMissingPermissions { granted ->
                    playerViewModel?.updatePermissionsState(granted, context)
                }
            } else {
                playerViewModel?.updatePermissionsState(true, context)
            }
        }

        DisposableEffect(videoDetails) {
            playerViewModel?.initializePlayer(context, playerConfig, videoDetails, entryIDToPlay, authorizationToken, analyticsViewerId)
            playerBind = playerViewModel?.player
            onDispose {
                if (isJetpackCompose) {
                    playerViewModel?.unbindAndStopService(context)
                } else {
                    playerViewModel?.handleAppInBackground(context)
                }
            }
        }

        val isReady = playerViewModel?.isPlayerReady?.collectAsState()

        key(videoDetails) {
            // Force recomposition when the video details list changes
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    playerView = PlayerView(context, playerViewModel?.player).apply {
                        keepScreenOn = true
                        player = playerViewModel?.player
                    }
                    playerView!!
                },
                update = { view ->
                    if (isReady?.value == true) {
                        view.player = playerViewModel?.player
                        if (playerConfig.playbackConfig.fullscreenEnabled)
                            playerView?.setFullscreenHandler(fullscreenHandler)
                        playerView?.invalidate()
                        playerViewModel?.updateBackgroundService(context)
                    }
                }
            )

            if (playerConfig.playbackConfig.fullscreenRotationEnabled)
                DetectRotationAndFullscreen(playerView) { isFullscreen ->
                    if (isFullscreen) {
                        playerView?.enterFullscreen()
                    } else {
                        playerView?.exitFullscreen()
                    }
                }

            DisposableEffect(currentLifecycle, configuration) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            playerViewModel?.handleAppInBackground(context)
                        }
                        Lifecycle.Event.ON_START -> {
                            playerViewModel?.handleAppInForeground(context)
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            playerViewModel?.handleAppInBackground(context)
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            playerViewModel?.handleAppInForeground(context)
                        }
                        else -> {}
                    }
                }
                currentLifecycle.lifecycle.addObserver(observer)

                onDispose {
                    currentLifecycle.lifecycle.removeObserver(observer)
                }
            }
        }

        if (fullscreen.value) {
            LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            SystemBars(false)
        } else {
            LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
            SystemBars(true)
        }
    }

    private fun listenToPlayerEvents() {
        CoroutineScope(Dispatchers.Main).launch {
            callbackFlow {
                playerBind?.on(PlayerEvent.Ready::class.java) { event ->
                    if (isListenerActive) {
                        trySend(event)
                    }
                }

                playerBind?.on(PlayerEvent.PlaylistTransition::class.java) { event ->
                    if (isListenerActive) {
                        trySend(event)
                    }
                }

                awaitClose {
                    isListenerActive = false
                }
            }.collect { event ->
                withContext(Dispatchers.Main) {
                    _eventFlow.emit(event)
                }
            }
        }
    }

    @Composable
    fun LockScreenOrientation(orientation: Int) {
        val context = LocalContext.current
        DisposableEffect(orientation) {
            val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
            activity.requestedOrientation = orientation
            onDispose {}
        }
    }

    @Composable
    fun SystemBars(show: Boolean) {
        val context = LocalContext.current
        val activity = context.findActivity() ?: return
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            if (show) {
                show(WindowInsetsCompat.Type.statusBars())
                show(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun RequestMissingPermissions(callback: ((Boolean) -> Unit)) {
        val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS) { granted ->
            callback(granted)
        }
        if (!permissionState.status.isGranted) {
            LaunchedEffect(
                key1 = Unit,
                block = { permissionState.launchPermissionRequest() }
            )
        } else {
           callback(true)
        }
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    override fun play() {
        playerBind?.play()
    }

    override fun pause() {
        playerBind?.pause()
    }

    override fun playNext() {
        playerBind?.playlist?.sources?.let { sources ->
            val index = sources.indexOfFirst { it.isActive }
            if (index != -1) {
                val nextIndex = index.plus(1)
                if (nextIndex < sources.size) {
                    val nextSource = sources[nextIndex]
                    seekSource(nextSource)
                }
            }
        }
    }

    override fun playPrevious() {
        playerBind?.playlist?.sources?.let { sources ->
            val index = sources.indexOfFirst { it.isActive }
            if (index > 0) {
                val nextIndex = index.minus(1)
                val prevSource = sources[nextIndex]
                seekSource(prevSource)
            }
        }
    }

    override fun playLast() {
        playerBind?.playlist?.sources?.last()?.let { lastSource ->
            seekSource(lastSource)
        }
    }

    override fun playFirst() {
        playerBind?.playlist?.sources?.first()?.let { firstSource ->
            seekSource(firstSource)
        }
    }

    override fun seek(entryId: String, completion: (Boolean) -> Unit) {
        playerBind?.playlist?.sources?.let { sources ->
            val index = sources.indexOfFirst {
                it.config.metadata?.get("entryId") == entryId
            }
            if (index != -1) {
                seekSource(sources[index]) {
                    completion(it)
                }
            } else {
                completion(false)
            }
        } ?: {
            completion(false)
        }
    }

    override fun activeEntryId(): String? {
        activeSource()?.let { source ->
            return source.config.metadata?.get("entryId")
        }

        return null
    }

    private fun activeSource(): Source? {
        playerBind?.playlist?.sources?.let { sources ->
            val index = sources.indexOfFirst { it.isActive }
            if (index != -1) {
                return sources[index]
            }
        }

        return null
    }

    private fun seekSource(source: Source, completion: ((Boolean) -> Unit)? = null) {
        playerBind?.playlist?.sources?.let { sources ->
            val index = sources.indexOfFirst { it.config.metadata?.get("entryId") == source.config.metadata?.get("entryId") }
            if (index != -1) {
                updateSource(sources[index]) { updatedSource ->
                    if (updatedSource != null) {
                        CoroutineScope(Job() + Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                playerBind?.playlist?.remove(index)
                                playerBind?.playlist?.add(updatedSource, index)
                                playerBind?.playlist?.seek(updatedSource, 0.0)
                            }
                        }
                        completion?.invoke(true)
                    } else {
                        completion?.invoke(false)
                    }
                }
            } else {
                completion?.invoke(false)
            }
        }
    }

    private fun updateSource(source: Source, completion: ((Source?) -> Unit)? = null) {
        val entryId = source.config.metadata?.get("entryId")
        val authorizationToken = source.config.metadata?.get("authorizationToken")
        
        if (entryId != null) {
            PlaybackSDKManager.loadHLSStream(
                entryId,
                authorizationToken
            ) { response, error ->
                if (error != null) {
                    completion?.invoke(null)
                } else {
                    val videoDetails = response?.toVideoDetails()
                    if (response != null && videoDetails != null) {
                        val newSource = PlaybackSDKManager.createSource(videoDetails, authorizationToken)
                        completion?.invoke(newSource)
                    } else {
                        completion?.invoke(null)
                    }
                }
            }
        } else {
            completion?.invoke(null)
        }
    }

    override fun removePlayer() {
        playerBind?.destroy()
    }

    override fun clean(context: Context) {
        playerViewModel?.unbindAndStopService(context=context)
        playerViewModel?.clean()
    }

    private val fullscreenHandler = object : FullscreenHandler {
        override fun onFullscreenRequested() {
            fullscreen.value = true
        }

        override fun onPause() {
        }

        override fun onResume() {
        }

        override val isFullscreen: Boolean
            get() = fullscreen.value

        override fun onDestroy() {
        }

        override fun onFullscreenExitRequested() {
            fullscreen.value = false
        }
    }
}
