package com.bwc.tul.websocket

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException
class WebSocketInterceptor(
    private val context: Context,
    private val baseUrl: String
) : Interceptor {
    private val logger = WebSocketLogger(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        scope.launch {
            logger.logSentMessage(request.url.toString(), "--> HTTP Upgrade Request: ${request.method} ${request.url}\n${request.headers}")
        }

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            scope.launch { logger.logError(request.url.toString(), "<-- HTTP Upgrade FAILED", e.stackTraceToString())}
            throw e
        }

        scope.launch {
            if (isWebSocketUpgrade(response)) {
                logger.logStatus(request.url.toString(), "<-- HTTP Upgrade Response: ${response.code} ${response.message}\n${response.headers}")
            } else {
                logger.logError(request.url.toString(), "<-- Non-websocket response", "Code: ${response.code}\nHeaders: ${response.headers}")
            }
        }
        return response
    }

    private fun isWebSocketUpgrade(response: Response): Boolean {
        return response.code == 101 &&
                response.header("Connection")?.equals("Upgrade", ignoreCase = true) == true &&
                response.header("Upgrade")?.equals("websocket", ignoreCase = true) == true
    }

    fun shutdown() {
        scope.coroutineContext.cancel()
    }
}