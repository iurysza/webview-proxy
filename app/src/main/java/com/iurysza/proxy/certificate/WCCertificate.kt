package com.iurysza.proxy.certificate

import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Provider
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder

class WCCertificate {
    lateinit var cert: X509Certificate
    lateinit var kPair: KeyPair

    private fun createCertificate(hostname: String): WCCertificate {
        kPair = generateKeyPair()
        cert = createSignerAndVerify(kPair, generateCertificate(hostname, kPair))
        return this
    }

    private fun createSignerAndVerify(
        keyPair: KeyPair,
        v3Builder: X509v3CertificateBuilder
    ): X509Certificate {
        val contentSigner =
            JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)
        val holder = v3Builder.build(contentSigner)
        val certificate = JcaX509CertificateConverter().setProvider(provider).getCertificate(holder)
        certificate.verify(keyPair.public)
        return certificate
    }

    private fun generateCertificate(
        hostname: String,
        keyPair: KeyPair
    ): X509v3CertificateBuilder {
        val owner = X500Name("CN=$hostname")
        val subject = X500Name("CN=" + hostname + ";" + SecureRandom())
        return X509v3CertificateBuilder(
            owner,
            BigInteger(64, SecureRandom()),
            DEFAULT_NOT_BEFORE,
            DEFAULT_NOT_AFTER,
            subject,
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        )
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(1024, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    companion object {
        /**
         * Current time minus 1 year, just in case software clock goes back due to time synchronization
         */
        private val DEFAULT_NOT_BEFORE = Date(System.currentTimeMillis() - 86400000L * 365)

        /**
         * The maximum possible value in X.509 specification: 9999-12-31 23:59:59
         */
        private val DEFAULT_NOT_AFTER = Date(253402300799000L)
        private val provider: Provider = BouncyCastleProvider()

        fun generateCert(hostname: String): WCCertificate {
            return WCCertificate().createCertificate(hostname)
        }
    }
}
