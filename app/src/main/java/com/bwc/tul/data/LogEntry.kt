package com.bwc.tul.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val level: String, // e.g., "INFO", "ERROR", "NETWORK"
    val tag: String,
    val message: String
)