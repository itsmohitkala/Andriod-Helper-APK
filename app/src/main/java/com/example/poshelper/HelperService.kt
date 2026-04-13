package com.example.poshelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class HelperService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val CHANNEL_ID = "pos_helper_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var server: LocalBridgeServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startHelper()
            ACTION_STOP -> stopHelper()
        }
        return START_STICKY
    }

    private fun startHelper() {
        createNotificationChannel()

        val notification: Notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("POS Helper")
                    .setContentText("Local helper is running on port 3001")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build()
            } else {
                Notification.Builder(this)
                    .setContentTitle("POS Helper")
                    .setContentText("Local helper is running on port 3001")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build()
            }

        startForeground(NOTIFICATION_ID, notification)

        if (server == null) {
            server = LocalBridgeServer(3001)
            server?.start()
        }
    }

    private fun stopHelper() {
        server?.stop()
        server = null
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "POS Helper Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}