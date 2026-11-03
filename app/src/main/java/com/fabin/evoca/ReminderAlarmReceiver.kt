package com.fabin.evoca

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

import androidx.core.app.NotificationCompat

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val reminderId =
            intent.getIntExtra(
                "REMINDER_ID",
                -1
            )

        if (reminderId == -1) {
            return
        }

        createChannel(context)

        if (intent.action == ACTION_ESCALATE) {

            showFullScreenNotification(
                context,
                reminderId
            )

        } else {

            showNormalNotification(
                context,
                reminderId
            )

            scheduleEscalation(
                context,
                reminderId
            )
        }
    }

    companion object {

        const val CHANNEL_ID =
            "evoca_alarm_channel"

        const val ACTION_ESCALATE =
            "EVoca_ESCALATION"

        private const val ESCALATION_REQUEST_OFFSET =
            1_000_000

        private const val ESCALATION_DELAY =
            5 * 60 * 1000L

        // ---------------------------------------------------------
        // NOTIFICATION CHANNEL
        // ---------------------------------------------------------

        fun createChannel(
            context: Context
        ) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                val soundUri =
                    Uri.parse(
                        "android.resource://${context.packageName}/${R.raw.evoca_chime}"
                    )

                val audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()

                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        "Evoca Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {

                        description =
                            "Reminder notifications from Evoca"

                        enableVibration(true)

                        setShowBadge(true)

                        setSound(
                            soundUri,
                            audioAttributes
                        )
                    }

                val notificationManager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                notificationManager.createNotificationChannel(
                    channel
                )
            }
        }

        // ---------------------------------------------------------
        // REMINDER ACTIVITY PENDING INTENT
        // ---------------------------------------------------------

        private fun createAlarmPendingIntent(
            context: Context,
            reminderId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    ReminderAlarmActivity::class.java
                ).apply {

                    putExtra(
                        "REMINDER_ID",
                        reminderId
                    )
                }

            return PendingIntent.getActivity(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )
        }

        // ---------------------------------------------------------
        // ESCALATION PENDING INTENT
        // ---------------------------------------------------------

        private fun createEscalationPendingIntent(
            context: Context,
            reminderId: Int
        ): PendingIntent {

            val intent =
                Intent(
                    context,
                    ReminderAlarmReceiver::class.java
                ).apply {

                    action =
                        ACTION_ESCALATE

                    putExtra(
                        "REMINDER_ID",
                        reminderId
                    )
                }

            return PendingIntent.getBroadcast(
                context,
                reminderId +
                        ESCALATION_REQUEST_OFFSET,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )
        }

        // ---------------------------------------------------------
        // NORMAL NOTIFICATION
        // ---------------------------------------------------------

        private fun showNormalNotification(
            context: Context,
            reminderId: Int
        ) {

            val pendingIntent =
                createAlarmPendingIntent(
                    context,
                    reminderId
                )

            val notification =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(
                        R.mipmap.ic_launcher
                    )
                    .setContentTitle(
                        "Evoca"
                    )
                    .setContentText(
                        "Your reminder is due"
                    )
                    .setContentIntent(
                        pendingIntent
                    )
                    .setPriority(
                        NotificationCompat.PRIORITY_HIGH
                    )
                    .setCategory(
                        NotificationCompat.CATEGORY_REMINDER
                    )
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build()

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.notify(
                reminderId,
                notification
            )
        }

        // ---------------------------------------------------------
        // ESCALATION / FULL-SCREEN NOTIFICATION
        // ---------------------------------------------------------

        private fun showFullScreenNotification(
            context: Context,
            reminderId: Int
        ) {

            val pendingIntent =
                createAlarmPendingIntent(
                    context,
                    reminderId
                )

            val notification =
                NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(
                        R.mipmap.ic_launcher
                    )
                    .setContentTitle(
                        "Evoca"
                    )
                    .setContentText(
                        "Your reminder is still waiting"
                    )
                    .setContentIntent(
                        pendingIntent
                    )
                    .setPriority(
                        NotificationCompat.PRIORITY_MAX
                    )
                    .setCategory(
                        NotificationCompat.CATEGORY_ALARM
                    )
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setFullScreenIntent(
                        pendingIntent,
                        true
                    )
                    .build()

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.notify(
                reminderId,
                notification
            )
        }

        // ---------------------------------------------------------
        // SCHEDULE 5-MINUTE ESCALATION
        // ---------------------------------------------------------

        private fun scheduleEscalation(
            context: Context,
            reminderId: Int
        ) {

            val alarmManager =
                context.getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

            val escalationIntent =
                createEscalationPendingIntent(
                    context,
                    reminderId
                )

            val triggerTime =
                System.currentTimeMillis() +
                        ESCALATION_DELAY

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {

                // Exact alarm permission is unavailable,
                // so use a normal alarm as a fallback.

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    escalationIntent
                )

            } else {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    escalationIntent
                )
            }
        }

        // ---------------------------------------------------------
        // CANCEL 5-MINUTE ESCALATION
        // ---------------------------------------------------------

        fun cancelEscalation(
            context: Context,
            reminderId: Int
        ) {

            val alarmManager =
                context.getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

            val pendingIntent =
                createEscalationPendingIntent(
                    context,
                    reminderId
                )

            alarmManager.cancel(
                pendingIntent
            )
        }
    }
}