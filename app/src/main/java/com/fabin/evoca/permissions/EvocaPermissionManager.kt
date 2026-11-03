package com.fabin.evoca.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.content.ContextCompat

object EvocaPermissionManager {

    /**
     * Checks whether Evoca can send notifications.
     *
     * Android 13+ requires POST_NOTIFICATIONS.
     * Older Android versions do not require this runtime permission.
     */
    fun hasNotificationPermission(
        context: Context
    ): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks whether Evoca can schedule exact alarms.
     *
     * Android versions before Android 12 do not require
     * the special exact-alarm access check.
     */
    fun canScheduleExactAlarms(
        context: Context
    ): Boolean {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Checks whether Evoca can use full-screen intents.
     *
     * This capability is relevant on newer Android versions.
     * Older versions are treated as supported.
     */
    fun canUseFullScreenIntent(
        context: Context
    ): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        return notificationManager.canUseFullScreenIntent()
    }

    /**
     * Scheduled reminders require notifications.
     * Exact alarms are also required for precise scheduling.
     *
     * Full-screen intent is NOT considered mandatory.
     */
    fun canUseScheduledReminders(
        context: Context
    ): Boolean {

        return hasNotificationPermission(context) &&
                canScheduleExactAlarms(context)
    }
}