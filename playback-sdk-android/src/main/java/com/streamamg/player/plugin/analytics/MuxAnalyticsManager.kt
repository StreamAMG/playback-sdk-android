package com.streamamg.player.plugin.analytics

import android.content.Context
import com.bitmovin.player.PlayerView
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.muxstats.bitmovinplayer.MuxStatsSDKBitmovinPlayer

object MuxAnalyticsManager {

    private var muxStats: MuxStatsSDKBitmovinPlayer? = null

    fun track(
        context: Context,
        playerView: PlayerView,
        environmentKey: String,
        playerName: String,
        videoTitle: String,
        videoId: String,
        viewerId: String
    ) {
        val customerPlayerData = CustomerPlayerData().apply {
            this.environmentKey = environmentKey
            this.playerName = playerName
        }

        val customerVideoData = CustomerVideoData().apply {
            this.videoTitle = videoTitle
            this.videoId = videoId
        }

        val customerViewData = CustomerViewData().apply {
            this.viewSessionId = viewerId
        }

        val customerData = CustomerData(customerPlayerData, customerVideoData, customerViewData)

        muxStats = MuxStatsSDKBitmovinPlayer(context, playerView, playerName, customerData)
    }

    // Stop tracking and release resources
    fun release() {
        muxStats?.release()
        muxStats = null
    }
}