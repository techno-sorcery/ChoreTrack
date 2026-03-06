package com.example.choretracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK_NAME = "open_chores_reminder_work"
private const val STARTUP_REMINDER_WORK_NAME = "open_chores_startup_check"

class OpenChoresReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    private val gson = Gson()

    override fun doWork(): Result {
        val context = applicationContext
        if (!ReminderSettings.isEnabled(context)) return Result.success()

        val prefs = context.getSharedPreferences("choretracker", Context.MODE_PRIVATE)
        val choresJson = prefs.getString("chores", null) ?: return Result.success()

        val choresType = object : TypeToken<MutableList<Chore>>() {}.type
        val chores: List<Chore> = gson.fromJson(choresJson, choresType) ?: emptyList()

        val openChores = chores.filter { !it.completed }
        if (openChores.isEmpty()) return Result.success()

        val overdueCount = openChores.count { chore ->
            val dueAt = chore.dueAtMillis ?: return@count false
            isOverdue(dueAt)
        }

        val todayKey = localDayKey(System.currentTimeMillis())
        val lastReminderDay = prefs.getInt("reminder_last_day", -1)
        if (lastReminderDay == todayKey) return Result.success()

        if (!canPostNotifications(context)) return Result.success()

        createNotificationChannel(context)
        postReminderNotification(
            context = context,
            openCount = openChores.size,
            overdueCount = overdueCount
        )

        prefs.edit {
            putInt("reminder_last_day", todayKey)
        }

        return Result.success()
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Chore reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for open household chores"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun postReminderNotification(
        context: Context,
        openCount: Int,
        overdueCount: Int
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text =
            if (overdueCount > 0) {
                "You have $overdueCount overdue chore(s) and $openCount open chore(s)."
            } else {
                "You have $openCount open chore(s)."
            }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_checklist_24)
            .setContentTitle("Chore reminder")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun isOverdue(dueAtMillis: Long): Boolean {
        return localDayKey(dueAtMillis) < localDayKey(System.currentTimeMillis())
    }

    private fun localDayKey(timeMillis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val CHANNEL_ID = "open_chores_reminders"
        private const val NOTIFICATION_ID = 22001
    }
}

object ReminderScheduler {

    fun schedule(context: Context) {
        if (!ReminderSettings.isEnabled(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(STARTUP_REMINDER_WORK_NAME)
            return
        }

        val reminderHour = ReminderSettings.reminderHour(context)
        val request =
            PeriodicWorkRequestBuilder<OpenChoresReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(
                    millisUntilNextLocalHour(reminderHour),
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        val startupRequest =
            OneTimeWorkRequestBuilder<OpenChoresReminderWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            STARTUP_REMINDER_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            startupRequest
        )
    }

    private fun millisUntilNextLocalHour(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return next.timeInMillis - now.timeInMillis
    }
}

object ReminderSettings {
    private const val PREFS_NAME = "choretracker"
    private const val KEY_ENABLED = "reminders_enabled"
    private const val KEY_HOUR = "reminders_hour"
    private const val DEFAULT_ENABLED = true
    private const val DEFAULT_HOUR = 9

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
    }

    fun reminderHour(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HOUR, DEFAULT_HOUR).coerceIn(0, 23)
    }

    fun save(context: Context, enabled: Boolean, hour: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_ENABLED, enabled)
            putInt(KEY_HOUR, hour.coerceIn(0, 23))
        }
    }
}
