package com.iurysza.proxy


import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.KeyStore
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import android.util.Log
import com.iurysza.proxy.certificate.WCCertificateRegistry
import rawhttp.core.RawHttp

class ProxyServer() : Thread() {

    companion object {
        const val TAG = "WVProxy.ProxyServer"
        var DEFAULT_QUEUE_SIZE = 4096
        private val REQUEST_QUEUE: ArrayBlockingQueue<Runnable> = ArrayBlockingQueue(DEFAULT_QUEUE_SIZE)
        private const val CONNECT = "CONNECT"

        /**
         * This pool size is confronted by the following formula: threads = number of cores * (1 + wait time / service time)
         * Assumption is that our tasks are taking 100ms of wait time and 20ms of CPU time. For a 4 cored device that make 24 threads.
         */
        private var POOL_SIZE = Runtime.getRuntime().availableProcessors() * 6
    }

    private lateinit var proxySocket: ServerSocket
    val proxyHost = "localhost"
    var proxyPort = 0
    var isRunning = false


    /**
     * Set up connection thread pool. Allow max 24 threads and max 2048 connections to be made.
     *
     */
    private var connectionPool: ThreadPoolExecutor? = null

    override fun run() {
        while (isRunning) {
            Log.e(TAG, "run: $isRunning")
            var clientSocket: Socket? = null
            try {
                clientSocket = proxySocket.accept()
            } catch (e: Exception) {
                Log.e(TAG, "run:", e)
            }
            connectionPool?.execute(clientSocket?.let { ConnectionHandler(it) })
//            if (ProxyUtils.isLocal(clientSocket.inetAddress) && connectionPool != null && !connectionPool!!.isShutdown) {
//            } else {
//                clientSocket.close()
//            }
        }
    }

    fun startServer() {
        connectionPool =
            ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 3000L, TimeUnit.MILLISECONDS, REQUEST_QUEUE)
        isRunning = true
        proxySocket = ServerSocket(proxyPort)
        proxyPort = proxySocket.localPort
        Log.e(TAG, "startServer: ")
        this.start()
    }

    fun stopServer() {
        isRunning = false
        connectionPool?.shutdown()
    }

    /**
     * Main thread to handle incoming connection request to proxy server
     *
     * @param client socket for client connection
     */
    class ConnectionHandler(private var client: Socket) : Runnable {
        private val rawHttpParser = RawHttp()
        override fun run() {
            try {
                handle()
            } catch (ex: Exception) {
                Log.d(TAG, ex.message.toString())
            } finally {
                client.close()
            }
        }

        private fun handle() {
            Log.e(TAG, "handle: ")
            val requestLine = RequestParser.parseRequestLine(client.getInputStream())
            requestLine?.let { request ->
                if (request.action == CONNECT && request.uri != null) {
                    val hostAndPort = ProxyUtils.parseUrl(request.uri!!)
                    ProxyUtils.writeConnectionEstablished(client.outputStream)
                    initSSLConnection(client, hostAndPort.host, hostAndPort.port)
                }
            }
        }

        private fun initSSLConnection(client: Socket, host: String, port: Int) {
            val factory: SSLSocketFactory = initSSLFactory(host)
            val sslSocket: SSLSocket =
                factory.createSocket(client, null, client.port, true) as SSLSocket
            sslSocket.useClientMode = false
            try {
                sslSocket.startHandshake()
                val remote = SSLContext.getDefault().socketFactory.createSocket() as SSLSocket
                remote.soTimeout =
                    20000 //How much read() can block this socket.(Some downloads might take a while)
                remote.connect(InetSocketAddress(host, port), 10000)
                object : Thread() {
                    //Delivers remote response to webView
                    override fun run() {
                        try {
                            linkResponses(host, remote.inputStream, sslSocket.outputStream)
                        } catch (ex: SocketException) {
                            Log.d(TAG, "Socket is closed")
                        } finally {
                            sslSocket.close()
                            remote.close()
                        }
                    }
                }.start()
                try {
                    linkRequests(sslSocket.inputStream, remote.outputStream)
                } catch (ex: SocketException) {
                    Log.d(TAG, "Socket is closed")
                } finally {
                    sslSocket.close()
                    remote.close()
                }
            } catch (ex: IOException) {//To catch SSL UNKNOWN CERTIFICATE ALERT
                client.close()
                sslSocket.close()
            }
        }


        /**
         * Adds necessary self-signed certificate to keystore of initialized SSLContext.
         *
         * @param host hostname for self-signed certificate
         */
        private fun initSSLFactory(host: String): SSLSocketFactory {
            val ks: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null, "pass".toCharArray())

            var certificate = WCCertificateRegistry.getCert(host)
            if (certificate == null) {
                certificate = WCCertificateRegistry.createCertificate(host)
                WCCertificateRegistry.putCert(host, certificate)
            }
            ks.setCertificateEntry("CA", certificate.cert)
            ks.setKeyEntry(
                "key-alias",
                certificate.kPair.private,
                "pass".toCharArray(),
                arrayOf(certificate.cert)
            )

            val keyManagerFactory = KeyManagerFactory.getInstance("X509")
            keyManagerFactory.init(ks, "pass".toCharArray())

            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(keyManagerFactory.keyManagers, null, null)
            return sslContext.socketFactory
        }

        /**
         * Connects two socket streams to each other as a bridge.
         *
         * @param inp stream to read incoming requests
         * @param out stream to write outgoing requests
         */
        private fun linkRequests(inp: InputStream, out: OutputStream) {
            try {
                val buffer = ByteArray(1024)
                var read: Int
                while (inp.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    out.flush()
                }
            } catch (streamEx: Exception) {
                Log.d(TAG, streamEx.message.toString())
            } finally {
                inp.close()
                out.close()
            }
        }

        private fun linkResponses(host: String, inp: InputStream, out: OutputStream) {
            try {
                val response = rawHttpParser.parseResponse(inp).eagerly()
                Log.e(TAG, "linkResponses: ${response.headers["user-agent"]}")
                response.writeTo(out)
                out.flush()
            } catch (streamEx: Exception) {
                Log.d(TAG, streamEx.message.toString())
            } finally {
                inp.close()
                out.close()
            }
        }
    }
}
