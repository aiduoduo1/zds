package com.jj.zds.feature.message

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jj.zds.R
import com.jj.zds.feature.pda.PdaMainActivity

class ZdsMessageNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun buildForegroundNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.zds_ic_notification)
            .setContentTitle("宗益科技")
            .setContentText("消息进程守护中")
            .setContentIntent(createMainIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createMainIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            REQUEST_MAIN,
            Intent(context, PdaMainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Message connection",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private companion object {
        const val SERVICE_CHANNEL_ID = "zds_message_service"
        const val REQUEST_MAIN = 10001
    }
}
