package com.iurysza.proxy

import android.net.http.SslError
import com.iurysza.proxy.certificate.WCCertificateRegistry

class CustomProxyController(
    private val webViewProxy: WebViewProxy,
    private val wcCertificate: WCCertificateRegistry.Companion = WCCertificateRegistry.Companion
) {
    private val bannedList: List<String> = emptyList()

    fun startProxyIfNeeded(host: String?): Boolean {
        host ?: return false
//        if (host == null || !shouldProxyHost(host)) return false
        webViewProxy.start()
        if (wcCertificate.getCert(host) != null) {
            synchronized(this) {
                wcCertificate.putCert(
                    host,
                    wcCertificate.createCertificate(host)
                )
            }
        }
        return true
    }

    fun checkSslError(error: SslError, onIgnoreError: () -> Unit) {
        val certProps = error.certificate.issuedTo.cName.split(";")
        if (certProps.size > 1) {
            val issuedTo = certProps[0]
            val checkRandom = certProps[1]
            if (wcCertificate.checkIfTrusted(issuedTo, checkRandom)) {
                onIgnoreError()
                return
            }
        }
    }

    private fun shouldProxyHost(host: String): Boolean = host in bannedList

}
