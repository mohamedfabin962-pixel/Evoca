package com.fabin.evoca.data

import kotlinx.coroutines.flow.first

class ReminderRepository(
    private val reminderDao: ReminderDao
) {

    suspend fun addReminder(
        reminder: Reminder
    ) {
        reminderDao.insertReminder(
            reminder
        )
    }


    suspend fun getScheduledReminders(): List<Reminder> {
        return reminderDao
            .observeScheduledReminders()
            .first()
    }


    suspend fun getQuickReminders(): List<Reminder> {
        return reminderDao
            .observeQuickReminders()
            .first()
    }


    suspend fun deleteReminder(
        reminder: Reminder
    ) {
        reminderDao.deleteReminder(
            reminder
        )
    }


    suspend fun updateReminder(
        reminder: Reminder
    ) {
        reminderDao.updateReminder(
            reminder
        )
    }
}