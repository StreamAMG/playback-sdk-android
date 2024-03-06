//package com.streamamg
//// Replace with your library's package name
//
//import android.content.Context
//import android.util.AttributeSet
//import com.bitmovin.player.api.Player
//import com.bitmovin.player.api.source.SourceConfig
//import com.bitmovin.player.ui.BitmovinPlayerView
//import kotlinx.coroutines.flow.internal.NoOpContinuation.context
//import kotlin.coroutines.jvm.internal.CompletedContinuation.context
//
//class BitmovinPlayerView @JvmOverloads constructor(
//    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
//) : BitmovinPlayerView(context, attrs, defStyleAttr) {
//
//    private var player: Player? = null
//
//    /**
//     * Initialize the Bitmovin player with the provided source URL.
//     *
//     * @param sourceUrl The URL of the media source to load.
//     * @param analyticsKey (Optional) The Bitmovin analytics license key.
//     */
//    fun initializePlayer(sourceUrl: String, analyticsKey: String? = null) {
//        player = Player(context)
//
//        player?.load(SourceConfig.fromUrl(sourceUrl))
//        setPlayer(player) // Set the player instance to the view
//    }
//
//    /**
//     * Release the player resources.
//     */
//    fun releasePlayer() {
//        player?.release()
//        player = null
//    }
//
//    // Expose other player control methods as needed (play, pause, etc.)
//}
