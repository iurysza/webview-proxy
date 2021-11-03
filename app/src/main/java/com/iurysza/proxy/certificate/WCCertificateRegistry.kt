package com.iurysza.proxy.certificate

import com.iurysza.proxy.ProxyUtils

class WCCertificateRegistry {
    companion object {
        private val certDict = HashMap<String, WCCertificate>()
        fun getCert(host: String): WCCertificate? {
            return certDict[ProxyUtils.parseUrl(host).host]
        }

        fun putCert(host: String, cert: WCCertificate) {
            certDict[ProxyUtils.parseUrl(host).host] = cert
        }

        fun createCertificate(host: String): WCCertificate {
            val cert: WCCertificate
            if (certDict.containsKey(ProxyUtils.parseUrl(host).host)) {
                cert = certDict[ProxyUtils.parseUrl(host).host]!!
            } else {
                cert = WCCertificate.generateCert(ProxyUtils.parseUrl(host).host)
                certDict[ProxyUtils.parseUrl(host).host] = cert
            }

            return cert
        }

        fun clearAll() {
            certDict.clear()
        }

        fun checkIfTrusted(issuedTo: String, checkRandom: String): Boolean {
            val cert = getCert(issuedTo)
            cert?.let {
                return it.cert.subjectDN.name.contains(checkRandom)
            }
            return false
        }
    }
}
