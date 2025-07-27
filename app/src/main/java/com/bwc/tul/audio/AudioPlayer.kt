package com.bwc.tul.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class AudioPlayer {

    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)

    @Volatile private var isReleased = false

    companion object {
        const val TAG = "AudioPlayer"
        private const val SAMPLE_RATE = 24000
    }

    init {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            Log.d(TAG, "AudioTrack initialized and playing.")
            startConsumingAudio() // Start the single consumer coroutine
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
        }
    }

    private fun startConsumingAudio() {
        scope.launch {
            audioQueue.consumeAsFlow().collect { audioData ->
                if (!isReleased && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        audioTrack?.write(audioData, 0, audioData.size)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to write audio data to track", e)
                    }
                }
            }
        }
    }

    fun playAudio(base64Audio: String) {
        if (isReleased) {
            Log.w(TAG, "AudioPlayer is released, skipping audio chunk.")
            return
        }
        scope.launch {
            try {
                val decodedData = Base64.decode(base64Audio, Base64.DEFAULT)
                audioQueue.send(decodedData) // Send data to the channel instead of writing directly
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Failed to decode Base64 audio chunk", e)
            }
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        Log.d(TAG, "Releasing AudioTrack...")
        scope.launch {
            audioQueue.close() // Close the channel
            try {
                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack?.flush()
                    audioTrack?.stop()
                }
                audioTrack?.release()
                audioTrack = null
                Log.d(TAG, "AudioTrack released successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception while releasing AudioTrack", e)
            }
        }
    }
}
