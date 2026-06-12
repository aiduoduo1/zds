package com.jj.zds.feature.message

import android.util.Log
import com.jj.network.JiangNetwork
import com.jj.network.sse.JSseClient

object ZdsMessageConnection {

    private val lock = Any()
    private var sseClient: JSseClient? = null
    private var started = false

    fun start() {
        synchronized(lock) {
            if (started && sseClient != null) {
                Log.d(TAG, "message connection is alive, skip start")
                return
            }

            Log.d(TAG, "message connection start")
            started = true
            sseClient = JiangNetwork.createSseClient(
                path = MESSAGE_SSE_PATH,
                listener = object : JSseClient.Listener {
                    override fun onData(data: String) {
                        Log.d(TAG, "message data: $data")
                    }

                    override fun onError(error: Throwable) {
                        Log.e(TAG, "message connection error, keep alive and wait for reconnect", error)
                    }

                    override fun onClosed() {
                        Log.d(TAG, "message connection closed")
                    }
                },
            ).also { it.connect() }
        }
    }

    fun stop() {
        val clientToStop = synchronized(lock) {
            if (!started && sseClient == null) {
                Log.d(TAG, "message connection already stopped")
                return
            }

            Log.d(TAG, "message connection stop")
            started = false
            sseClient.also {
                sseClient = null
            }
        }

        clientToStop?.disconnect()
    }

    fun connect() = start()

    fun disconnect() = stop()

    private const val TAG = "ZdsMessageConnection"
    private const val MESSAGE_SSE_PATH = "i/infoMsgMonitor/reg"
}
