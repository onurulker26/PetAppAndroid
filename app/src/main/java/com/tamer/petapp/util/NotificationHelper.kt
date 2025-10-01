package com.tamer.petapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.tamer.petapp.R
import com.tamer.petapp.vaccination.VaccinationsActivity
import java.util.concurrent.TimeUnit
import android.app.AlarmManager
import android.content.BroadcastReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "vaccination_reminders"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_ID_WEEK = 2
        const val NOTIFICATION_ID_DAY = 3
        const val WORK_NAME = "vaccination_reminder_work"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Aşı Hatırlatmaları"
            val descriptionText = "Evcil hayvanınızın aşı hatırlatmaları"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDaily() {
        val workRequest = PeriodicWorkRequestBuilder<VaccinationReminderWorker>(
            1, TimeUnit.DAYS,
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun showNotification(title: String? = null, content: String? = null, notificationId: Int = NOTIFICATION_ID) {
        val intent = Intent(context, VaccinationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val defaultTitle = "Aşı Hatırlatması"
        val defaultContent = "Evcil hayvanınızın aşılarını kontrol etmeyi unutmayın!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: defaultTitle)
            .setContentText(content ?: defaultContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(notificationId, builder.build())
            }
        }
    }

    /**
     * Belirli bir zamanda tetiklenecek bildirim zamanlar
     * @param notificationId Bildirim için benzersiz ID
     * @param title Bildirim başlığı
     * @param content Bildirim içeriği
     * @param triggerTime Bildirimin tetikleneceği zaman (milisaniye cinsinden)
     */
    fun scheduleNotification(notificationId: Int, title: String, content: String, triggerTime: Long) {
        createNotificationChannel()
        
        // Bildirim için intent oluştur
        val intent = Intent(context, VaccinationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Bildirim yapısını oluştur
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // AlarmManager kullarak zamanlama yap
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Bildirim için WorkManager kullan
        val notificationData = Data.Builder()
            .putInt("notificationId", notificationId)
            .putString("title", title)
            .putString("content", content)
            .build()
        
        val currentTime = System.currentTimeMillis()
        val delay = triggerTime - currentTime
        
        if (delay > 0) {
            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(notificationData)
                .build()
            
            WorkManager.getInstance(context).enqueue(notificationWork)
        }
    }
}

/**
 * Zamanlanan bildirimleri gösteren Worker
 */
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    
    override fun doWork(): Result {
        val notificationId = inputData.getInt("notificationId", 0)
        val title = inputData.getString("title") ?: "Aşı Hatırlatması"
        val content = inputData.getString("content") ?: "Evcil hayvanınızın aşı zamanı geldi"
        
        val intent = Intent(applicationContext, VaccinationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(notificationId, builder.build())
            }
        }
        
        return Result.success()
    }
}

class VaccinationReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showNotification()
        return Result.success()
    }
} 