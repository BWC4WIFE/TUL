package com.bwc.tul.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bwc.tul.data.websocket.WebSocketLogEntry
import java.util.Date
@TypeConverters(AppDatabase.Converters::class) // Reference nested Converters class
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun entryDao(): EntryDao
    abstract fun webSocketLogDao(): WebSocketLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "translator_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `websocket_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `direction` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `is_error` INTEGER NOT NULL DEFAULT 0,
                        `error_message` TEXT
                    )
                """)
            }
        }
    }

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? = date?.time
    }
}