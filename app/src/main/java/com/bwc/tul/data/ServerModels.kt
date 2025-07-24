package com.bwc.tul.data

import com.google.gson.annotations.SerializedName
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.ForeignKey
import androidx.room.Index


// API Response Models
data class ServerResponse(
    @SerializedName("serverContent") val serverContent: ServerContent?,
    @SerializedName("inputTranscription") val inputTranscription: Transcription?,
    @SerializedName("outputTranscription") val outputTranscription: Transcription?,
    @SerializedName("setupComplete") val setupComplete: SetupComplete?,
    @SerializedName("sessionResumptionUpdate") val sessionResumptionUpdate: SessionResumptionUpdate?,
    @SerializedName("goAway") val goAway: GoAway?
)

data class ServerContent(
    @SerializedName("parts") val parts: List<Part>?,
    @SerializedName("modelTurn") val modelTurn: ModelTurn?,
    @SerializedName("inputTranscription") val inputTranscription: Transcription?,
    @SerializedName("outputTranscription") val outputTranscription: Transcription?,
    @SerializedName("turnComplete") val turnComplete: Boolean?
)

data class ModelTurn(@SerializedName("parts") val parts: List<Part>?)
data class Part(@SerializedName("text") val text: String?, @SerializedName("inlineData") val inlineData: InlineData?)
data class InlineData(@SerializedName("mime_type") val mimeType: String?, @SerializedName("data") val data: String?)
data class Transcription(@SerializedName("text") val text: String?)
data class SetupComplete(val dummy: String? = null)
data class SessionResumptionUpdate(@SerializedName("newHandle") val newHandle: String?, @SerializedName("resumable") val resumable: Boolean?)
data class GoAway(@SerializedName("timeLeft") val timeLeft: String?)

