package com.iurysza.proxy.util

import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import android.content.Context
import com.google.android.gms.security.ProviderInstaller
import com.iurysza.proxy.certificate.WCCertificateRegistry

object TLSUtils {

    fun updateAndSetupSecurityProtocols(applicationContext: Context) {
        //Installs latest security providers if necessary - TLSv1.2
        ProviderInstaller.installIfNeeded(applicationContext)

        //Setting default SSLContext with TLSv1.2
        val sslContext = SSLContext.getInstance("TLSv1.2")

        //Passing null means; 'use default values'
        sslContext.init(null, null, null)
        SSLContext.setDefault(sslContext)
    }

    fun createSafeSocketConnection(client: Socket, host: String, port: Int): Pair<SSLSocket, SSLSocket> {
        val sslSocketFactory = initSSLFactory(host)
        return buildSockets(host, port, sslSocketFactory, client)
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

    private fun buildSockets(
        host: String,
        port: Int,
        factory: SSLSocketFactory,
        client: Socket
    ): Pair<SSLSocket, SSLSocket> {
        val clientSSLSocket = factory.createSocket(client, null, client.port, true) as SSLSocket
        clientSSLSocket.useClientMode = false
        // handshakes with proxy server
        runCatching {
            clientSSLSocket.startHandshake()
        }.onFailure {
            if (it !is SSLException) throw it
        }

        val targetSSLSocket = factory.createSocket() as SSLSocket
        targetSSLSocket.soTimeout = 20000
        targetSSLSocket.connect(InetSocketAddress(host, port), 10000)

        return clientSSLSocket to targetSSLSocket
    }
}
