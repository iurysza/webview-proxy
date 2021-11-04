package com.iurysza.proxy

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.Optional
import kotlin.concurrent.thread
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iurysza.TAG
import com.iurysza.proxy.util.ProxyUtils
import com.iurysza.proxy.util.RequestParser
import com.iurysza.proxy.util.ScriptFormatter
import com.iurysza.proxy.util.TLSUtils
import rawhttp.core.EagerHttpResponse
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpHeaders
import rawhttp.core.RawHttpRequest
import rawhttp.core.body.EagerBodyReader
import rawhttp.core.body.StringBody


/**
 * Main thread to handle incoming connection request to proxy server
 *
 * @param client socket for client connection
 */
@RequiresApi(Build.VERSION_CODES.Q)
class ConnectionHandler(
    private var client: Socket,
    private val rawHttpParser: RawHttp,
    private val scriptFormatter: ScriptFormatter
) : Runnable {

    override fun run() {
        try {
            handle()
        } catch (ex: Exception) {
            Log.e(ProxyServer.TAG, ex.message.toString(), ex)
        } finally {
            client.close()
        }
    }

    private fun handle() {
        val request = RequestParser.parseRequestLine(client.getInputStream()) ?: return
        if (request.action != "CONNECT" || request.uri == null) return

        val (host, port) = ProxyUtils.parseUrl(request.uri!!).run { host to port }
        ProxyUtils.writeConnectionEstablished(client.outputStream)

        val (sslSocketClient, sslSocketServer) = TLSUtils.createSafeSocketConnection(client, host, port)
        bindSockets(sslSocketClient, sslSocketServer, host)
    }

    private fun bindSockets(
        sslSocketClient: Socket,
        sslSocketServer: Socket,
        host: String
    ) {

        bindServerResponseToClient(host, sslSocketServer.inputStream, sslSocketClient.outputStream)
        bindClientRequestToServer(sslSocketClient.inputStream, sslSocketServer.outputStream)
    }

    private fun bindServerResponseToClient(
        host: String,
        inputStream: InputStream,
        outputStream: OutputStream
    ) {
        thread {
            try {
                parseResponse(inputStream, host).writeTo(outputStream)
                outputStream.flush()
            } catch (ex: Exception) {
                Log.e(ProxyServer.TAG, "${ex.message}")
            } finally {
                inputStream.close()
                outputStream.close()
            }
        }
    }

    private fun parseResponse(
        inp: InputStream,
        host: String
    ): EagerHttpResponse<Void> {
        val response = rawHttpParser.parseResponse(inp).eagerly()
        Log.e(TAG, "parseResponse: ${response.headers}")
        if (!response.headers.isHtmlContentType()) return response

        val updatedBody = runCatching {
            response.body.injectScript(scriptFormatter.getScriptsInHead(host))
        }.getOrNull()

        val updatedHeader = RawHttpHeaders.newBuilder()
            .with("Transfer-Encoding", " ")
            .with("Content-Encoding", " ")
            .build()

        return response
            .withBody(updatedBody)
            .withHeaders(updatedHeader)
            .eagerly()
    }

    /**
     * Connects two socket streams to each other as a bridge.
     *
     * @param inp stream to read incoming requests
     * @param out stream to write outgoing requests
     */
    private fun bindClientRequestToServer(inp: InputStream, out: OutputStream) {
        try {
            val buffer = ByteArray(4096)
            while (inp.read(buffer) != -1) {
                updateRequestHeaders(buffer)?.writeTo(out)
                out.flush()
            }
        } catch (streamEx: Exception) {
            Log.e(ProxyServer.TAG, streamEx.message.toString(), streamEx)
        } finally {
            inp.close()
            out.close()
        }
    }

    private fun updateRequestHeaders(buffer: ByteArray): RawHttpRequest? = rawHttpParser
        .parseRequest(ByteArrayInputStream(buffer))
        .withHeaders(
            RawHttpHeaders
                .newBuilder()
                .with("x-requested-with", "RANDOMISED_PACKAGE_NAME")
                .build()
        )

    private fun Optional<EagerBodyReader>.injectScript(headScript: String): StringBody {
        return StringBody(
            get()
                .decodeBodyToString(Charset.defaultCharset())
                .replace(
                    "<head>",
                    "<head>$headScript"
                ),
            "text/html;charset=utf-8"
        )
    }

    private fun RawHttpHeaders.isHtmlContentType(): Boolean {
        getFirst("Content-Type").apply {
            return isPresent && get().contains("html")
        }
    }
}
