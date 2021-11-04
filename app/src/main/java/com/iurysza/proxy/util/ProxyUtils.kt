package com.iurysza.proxy.util

import java.io.OutputStream
import java.net.InetAddress

object ProxyUtils {
    private const val CONNECTION_OK = "HTTP/1.1 200 Connection Established"

    /**
     * Checking if the requesting address belongs to local device
     *
     * @param address Address of the client
     */
    fun isLocal(address: InetAddress): Boolean {
        val ip = address.toString()
        return address.isAnyLocalAddress || address.isLoopbackAddress || ip.endsWith("::1") || ip.endsWith("127.0.0.1")
    }

    data class HostPort(val host: String, val port: Int)

    fun parseUrl(url: String): HostPort {
        if (url.contains(":")) {
            val hostAndPort = url.split(":")
            return HostPort(hostAndPort[0], hostAndPort[1].toInt())
        }
        return HostPort(url, 443)
    }

    fun writeConnectionEstablished(out: OutputStream) {
        out.write(CONNECTION_OK.toByteArray())
        out.write("\n\n".toByteArray())
        out.flush()
    }
}
