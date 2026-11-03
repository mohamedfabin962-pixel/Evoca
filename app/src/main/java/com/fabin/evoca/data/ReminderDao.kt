package com.fabin.evoca.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert
    suspend fun insertReminder(reminder: Reminder): Long


    // ---------------------------------------------------------
    // GET REMINDER BY ID
    // ---------------------------------------------------------

    @Query(
        "SELECT * FROM reminders " +
                "WHERE id = :reminderId " +
                "LIMIT 1"
    )
    suspend fun getReminderById(reminderId: Int): Reminder?


    // ---------------------------------------------------------
    // SCHEDULED REMINDERS
    // ---------------------------------------------------------

    @Query(
        "SELECT * FROM reminders " +
                "WHERE status = 'ACTIVE' " +
                "AND type = 'SCHEDULED' " +
                "ORDER BY reminderTime ASC"
    )
    fun observeScheduledReminders(): Flow<List<Reminder>>


    // ---------------------------------------------------------
    // QUICK REMINDERS
    // ---------------------------------------------------------

    @Query(
        "SELECT * FROM reminders " +
                "WHERE status = 'ACTIVE' " +
                "AND type = 'QUICK' " +
                "ORDER BY createdAt DESC"
    )
    fun observeQuickReminders(): Flow<List<Reminder>>


    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    @Update
    suspend fun updateReminder(reminder: Reminder)


    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @Delete
    suspend fun deleteReminder(reminder: Reminder)


    // ---------------------------------------------------------
    // CLEAN EXPIRED REMINDERS
    // ---------------------------------------------------------

    @Query(
        "DELETE FROM reminders " +
                "WHERE expiresAt < :currentTime"
    )
    suspend fun deleteExpiredReminders(currentTime: Long)
}