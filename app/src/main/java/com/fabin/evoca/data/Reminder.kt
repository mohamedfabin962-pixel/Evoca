package com.fabin.evoca.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // SCHEDULED or QUICK
    val type: String,

    // Only used by Scheduled Reminders
    val url: String? = null,

    val reminderMessage: String,

    // Instagram, YouTube, Website
    // Quick reminders don't need a source
    val source: String? = null,

    // Only used by Scheduled Reminders
    val reminderTime: Long? = null,

    val createdAt: Long,

    val expiresAt: Long,

    val status: String = "ACTIVE"
)