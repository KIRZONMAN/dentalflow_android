package com.dentalflow.myapplication.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {
    private const val CHANNEL_ID = "dentalflow_events"
    private const val CHANNEL_NAME = "DentalFlow eventos"
    private const val CHANNEL_DESC = "Notificaciones de creación/edición/borrado"

    private val nextId = AtomicInteger(1)

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = CHANNEL_DESC }
                nm.createNotificationChannel(ch)
            }
        }
    }

    /** true si tenemos permiso para notificar (o no se requiere en < 33) */
    fun canPost(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission") // ya verificamos canPost() arriba
    fun show(ctx: Context, title: String, message: String) {
        if (!canPost(ctx)) return

        ensureChannel(ctx)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(nextId.getAndIncrement(), notif)
        } catch (se: SecurityException) {
            Log.w("NotificationHelper", "Sin permiso POST_NOTIFICATIONS, no se notificó", se)
        }
    }
}
