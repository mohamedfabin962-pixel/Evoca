package com.fabin.evoca.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.NotificationManager

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.fabin.evoca.ReminderAlarmReceiver
import com.fabin.evoca.data.AppDatabase
import com.fabin.evoca.data.Reminder

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val reminderDao =
        AppDatabase
            .getDatabase(application)
            .reminderDao()


    // ---------------------------------------------------------
    // SCHEDULED REMINDERS
    // ---------------------------------------------------------

    val scheduledReminders:
            StateFlow<List<Reminder>> =
        reminderDao
            .observeScheduledReminders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )


    // ---------------------------------------------------------
    // QUICK REMINDERS
    // ---------------------------------------------------------

    val quickReminders:
            StateFlow<List<Reminder>> =
        reminderDao
            .observeQuickReminders()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )


    // ---------------------------------------------------------
    // ADD REMINDER
    // ---------------------------------------------------------

    fun addReminder(
        reminder: Reminder,
        onInserted: (Int) -> Unit
    ) {
        viewModelScope.launch {

            val insertedId =
                reminderDao.insertReminder(
                    reminder
                )

            // Room Flow automatically updates
            // scheduledReminders / quickReminders.

            onInserted(
                insertedId.toInt()
            )
        }
    }


    // ---------------------------------------------------------
    // DELETE REMINDER
    // ---------------------------------------------------------

    fun deleteReminder(
        reminder: Reminder
    ) {

        val context =
            getApplication<Application>()


        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager


        // Cancel the original scheduled alarm

        val alarmIntent =
            Intent(
                context,
                ReminderAlarmReceiver::class.java
            ).apply {

                putExtra(
                    "REMINDER_ID",
                    reminder.id
                )
            }


        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminder.id,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        alarmManager.cancel(
            pendingIntent
        )


        // Cancel the 5-minute escalation alarm

        ReminderAlarmReceiver.cancelEscalation(
            context,
            reminder.id
        )


        // Remove notification

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager


        notificationManager.cancel(
            reminder.id
        )


        // Delete from Room

        viewModelScope.launch {

            reminderDao.deleteReminder(
                reminder
            )

            // NO loadReminders()
            //
            // Room Flow automatically updates the UI.
        }
    }


    // ---------------------------------------------------------
    // UPDATE REMINDER
    // ---------------------------------------------------------

    fun updateReminder(
        reminder: Reminder
    ) {

        viewModelScope.launch {

            reminderDao.updateReminder(
                reminder
            )

            // Room Flow automatically updates the UI.
        }
    }


    // ---------------------------------------------------------
    // MARK AS DONE
    // ---------------------------------------------------------

    fun markAsDone(
        reminder: Reminder
    ) {

        viewModelScope.launch {

            val completedReminder =
                reminder.copy(
                    status = "DONE"
                )

            reminderDao.updateReminder(
                completedReminder
            )

            // Room Flow automatically removes
            // DONE reminders from the active list.
        }
    }
}