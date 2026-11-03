package com.fabin.evoca.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Reminder::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /*
         * Database migration:
         *
         * Version 1 → Version 2
         *
         * Existing reminders are treated as
         * SCHEDULED reminders because that is
         * what the old database contained.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {

                database.execSQL(
                    """
                    CREATE TABLE reminders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL DEFAULT 'SCHEDULED',
                        url TEXT,
                        reminderMessage TEXT NOT NULL,
                        source TEXT,
                        reminderTime INTEGER,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO reminders_new (
                        id,
                        type,
                        url,
                        reminderMessage,
                        source,
                        reminderTime,
                        createdAt,
                        expiresAt,
                        status
                    )
                    SELECT
                        id,
                        'SCHEDULED',
                        url,
                        reminderMessage,
                        source,
                        reminderTime,
                        createdAt,
                        expiresAt,
                        status
                    FROM reminders
                    """.trimIndent()
                )

                database.execSQL(
                    "DROP TABLE reminders"
                )

                database.execSQL(
                    "ALTER TABLE reminders_new RENAME TO reminders"
                )
            }
        }


        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "evoca_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}