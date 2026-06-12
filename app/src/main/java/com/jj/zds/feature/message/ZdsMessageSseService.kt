package com.jj.zds.feature.message

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

class ZdsMessageSseService : Service() {

    private lateinit var notificationHelper: ZdsMessageNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = ZdsMessageNotificationHelper(this)
        startForeground(FOREGROUND_NOTIFICATION_ID, notificationHelper.buildForegroundNotification())
        ZdsMessageConnection.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "message service start command")
        ZdsMessageConnection.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "message service destroy")
        ZdsMessageConnection.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ZdsMessageSseService"
        private const val FOREGROUND_NOTIFICATION_ID = 10001

        fun start(context: Context) {
            val intent = Intent(context, ZdsMessageSseService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ZdsMessageSseService::class.java))
        }
    }
}
