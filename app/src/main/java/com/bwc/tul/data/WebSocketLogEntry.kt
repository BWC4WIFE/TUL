package com.bwc.tul.data.websocket

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "websocket_logs")
data class WebSocketLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "direction") val direction: Direction,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "is_error") val isError: Boolean = false,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null
) {
    enum class Direction { SENT, RECEIVED, STATUS, ERROR }
}