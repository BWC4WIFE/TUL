package com.bwc.tul.data.websocket

import android.content.Context
import okhttp3.*
import okio.IOException
import com.bwc.tul.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Response
class WebSocketInterceptor(
    private val context: Context,
    private val baseUrl: String
) : Interceptor {
    private val logger = WebSocketLogger(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        return if (isWebSocketUpgrade(response)) {
            return response.newBuilder()
                .build()
        } else {
            response
        }
    }

    private fun isWebSocketUpgrade(response: Response): Boolean {
        return response.code == 101 &&
                response.header("Connection")?.equals("Upgrade", ignoreCase = true) == true &&
                response.header("Upgrade")?.equals("websocket", ignoreCase = true) == true
    }

    private fun createEventListener(url: String): EventListener {
        return object : EventListener() {
            override fun callStart(call: Call) {
                super.callStart(call)
                scope.launch {
                    logger.logSentMessage(url, "WebSocket connection initiated")
                }
            }

            override fun callEnd(call: Call) {
                super.callEnd(call)
                scope.launch {
                    logger.logSentMessage(url, "WebSocket connection closed")
                }
            }

            override fun callFailed(call: Call, ioe: IOException) {
                super.callFailed(call, ioe)
                scope.launch {
                    logger.logSentMessage(url, "WebSocket connection failed: ${ioe.message}")
                }
            }
        }
    }

    fun shutdown() {
        scope.coroutineContext.cancel()
    }
}