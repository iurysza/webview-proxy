package com.iurysza.proxy

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import android.os.Build
import androidx.annotation.RequiresApi
import com.iurysza.proxy.util.ProxyUtils
import com.iurysza.proxy.util.ScriptFormatter
import rawhttp.core.RawHttp

class ProxyServer(
    queueSize: Int?,
    private val scriptFormatter: ScriptFormatter
) : Thread() {

    private lateinit var proxySocket: ServerSocket
    private val requestQueue = ArrayBlockingQueue<Runnable>(queueSize ?: DEFAULT_QUEUE_SIZE)
    val proxyHost = "localhost"
    var proxyPort = 0

    @Volatile
    var isRunning: Boolean = false

    /**
     * Set up connection thread pool.
     * Allow max 24 threads and max 2048 connections to be made.
     */
    private val connectionPool: ThreadPoolExecutor by lazy {
        ThreadPoolExecutor(
            POOL_SIZE,
            POOL_SIZE,
            3000L,
            TimeUnit.MILLISECONDS,
            requestQueue
        )
    }

    fun startServer() {
        isRunning = true
        proxySocket = ServerSocket(proxyPort)
        proxyPort = proxySocket.localPort
        this.start()
    }

    fun stopServer() {
        isRunning = false
        connectionPool.shutdown()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun run() {
        while (isRunning) {
            val clientSocket: Socket = proxySocket.accept()
            if (socketIsValid(clientSocket)) {
                connectionPool.execute(
                    ConnectionHandler(clientSocket, RawHttp(), scriptFormatter)
                )
            } else {
                clientSocket.close()
            }
        }
    }


    private fun socketIsValid(clientSocket: Socket): Boolean {
        return ProxyUtils.isLocal(clientSocket.inetAddress) && !connectionPool.isShutdown
    }

    companion object {
        const val TAG = "WVProxy.ProxyServer"
        private var DEFAULT_QUEUE_SIZE = 4096

        /**
         * This pool size is confronted by the following formula: threads = number of cores * (1 + wait time / service time)
         * Assumption is that our tasks are taking 100ms of wait time and 20ms of CPU time. For a 4 cored device that make 24 threads.
         */
        private var POOL_SIZE = Runtime.getRuntime().availableProcessors() * 6
    }
}
