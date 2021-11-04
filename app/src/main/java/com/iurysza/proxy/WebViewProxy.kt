package com.iurysza.proxy

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.iurysza.proxy.certificate.WCCertificateRegistry
import com.iurysza.proxy.util.ScriptFormatter
import com.iurysza.proxy.util.TLSUtils

class WebViewProxy(
    applicationContext: Context,
    private val scriptFormatter: ScriptFormatter = ScriptFormatter(),
    private val config: WebViewProxyConfig = WebViewProxyConfig(),

    ) {

    private var proxyServer: ProxyServer? = null

    init {
        TLSUtils.updateAndSetupSecurityProtocols(applicationContext)
    }

    /**
     * Initializes proxy server and starts it.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun start() {
        if (config.enabled && !isProxyAlive()) {
            proxyServer = ProxyServer(config.queueSize, scriptFormatter).apply {
                startServer()
                //Configure proxy in system
                setWebViewProxy(
                    proxyHost,
                    proxyPort,
                    config.excludedHosts
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun shutDownProxy() {
        if (isProxySupported() && proxyServer?.isRunning != true) {
            ProxyController.getInstance().clearProxyOverride(PROXY_CHANGE_EXECUTOR, PROXY_CHANGE_LISTENER)
            proxyServer!!.stopServer()
            WCCertificateRegistry.clearAll()
        }
    }

    fun isProxyAlive() = proxyServer?.isRunning == true

    /**
     * Notifies the system that there is a proxy on specified host and port.
     *
     * This results as all the connection requests made in the webview
     * to be re-routed to specified configuration here.
     *
     * Excludes requests made for "file,*klarna.com"
     *
     * @param host  hostname of the proxy server
     * @param port  port of the proxy server
     */
    private fun setWebViewProxy(host: String, port: Int, excludedHosts: List<String>) {
        if (!isProxySupported()) return

        ProxyController.getInstance().setProxyOverride(
            proxyConfig(host, port, excludedHosts),
            PROXY_CHANGE_EXECUTOR,
            PROXY_CHANGE_LISTENER
        )
    }

    private fun proxyConfig(
        host: String,
        port: Int,
        excludedHosts: List<String>
    ): ProxyConfig = ProxyConfig
        .Builder()
        .apply {
            //excludes all connections other than https://
            addProxyRule("$host:$port", ProxyConfig.MATCH_HTTPS)
            // exclude pre-defined hosts.i.e klarna.com
            (preDefinedBypassRules + excludedHosts).forEach { addBypassRule(it) }
        }.build()

    private fun isProxySupported() = WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)

    companion object {
        private val PROXY_CHANGE_EXECUTOR = Executor {
            Handler(Looper.getMainLooper()).post(it)
        }
        private val PROXY_CHANGE_LISTENER = Runnable {}
        private val preDefinedBypassRules = CopyOnWriteArrayList<String>().apply {
            add("file://*")
            add("ws://*")
        }
    }
}
