package com.streamamg.player.ui

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.ui.notification.CustomActionReceiver
import com.bitmovin.player.api.ui.notification.NotificationListener
import com.bitmovin.player.ui.notification.DefaultMediaDescriptor
import com.bitmovin.player.ui.notification.PlayerNotificationManager
import com.streamamg.PlaybackSDKManager
import com.streamamg.playback_sdk_android.R

class BackgroundPlaybackService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "com.streamamg.playback"
        private const val NOTIFICATION_ID = 1
        private const val EMPTY_CHANNEL_DESCRIPTION = 0

        @Volatile
        var isRunning = false

        private var player: Player? = null

        fun releasePlayer() {
            player?.destroy()
            player = null
        }

        val sharedPlayer: Player?
            get() = player
    }

    // Binder given to clients
    private var bound = 0

    private var player: Player? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager

    private val customActionReceiver = object : CustomActionReceiver {
        override fun createCustomActions(context: Context): Map<String, NotificationCompat.Action> =
            emptyMap()

        override fun getCustomActions(player: Player) = if (!player.isPlaying && bound == 0) {
            listOf(PlayerNotificationManager.ACTION_STOP)
        } else {
            emptyList()
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            if (action == PlayerNotificationManager.ACTION_STOP) stopSelf()
        }
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class BackgroundBinder : Binder() {
        // Return this instance of Player so clients can use the player instance
        val player get() = sharedPlayer
        fun getService(): BackgroundPlaybackService = this@BackgroundPlaybackService
    }

    override fun onCreate() {
        super.onCreate()

        // Create a PlayerNotificationManager with the static create method
        // By passing null for the mediaDescriptionAdapter, a DefaultMediaDescriptionAdapter will be used internally.
        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.control_notification_channel,
            EMPTY_CHANNEL_DESCRIPTION,
            NOTIFICATION_ID,
            DefaultMediaDescriptor(assets),
            customActionReceiver
        ).apply {
            setNotificationListener(object : NotificationListener {
                override fun onNotificationStarted(
                    notificationId: Int,
                    notification: Notification
                ) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int) {
                    stopSelf()
                }
            })
        }
    }

    override fun onDestroy() {
        playerNotificationManager.setPlayer(null)
        releasePlayer()

        stopSelf()

        isRunning = false

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        bound++
        return BackgroundBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        bound--
        if (bound == 0 && player?.isPlaying == false) {
            stopSelf()
        }
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return START_STICKY
    }

    fun setPlayer(playerBind: Player?) {
        this.player = playerBind
        playerNotificationManager.setPlayer(playerBind)
    }
}
