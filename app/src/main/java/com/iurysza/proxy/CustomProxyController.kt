package com.iurysza.proxy

import java.util.concurrent.Executor
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.iurysza.proxy.ProxyServer.Companion.TAG

class CustomProxyController {
    private var proxyServer: ProxyServer? = null


    companion object {
        private val PROXY_CHANGE_EXECUTOR = object : Executor {
            private val handler = Handler(Looper.getMainLooper())
            override fun execute(r: Runnable) {
                Log.e(TAG, "execute: runnable")
                handler.post(r)
            }
        }
        private val PROXY_CHANGE_LISTENER = Runnable {}

    }

    fun applyProxyIfNeed(request: WebResourceRequest?) {
        if (request?.isForMainFrame != true) return

        runCatching {
            proxyServer = ProxyServer().apply {
                startServer()
                setProxy(proxyHost, proxyPort)
            }
        }.onFailure {
            Log.e(TAG, "applyProxyIfNeed: ", it)
        }.onSuccess {
            Log.e(TAG, "applyProxyIfNeed: applied", )
        }
    }

    private fun setProxy(host: String, port: Int) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyConfig = ProxyConfig.Builder()
                .apply {
                    addProxyRule(
                        "$host:$port",
                        ProxyConfig.MATCH_HTTPS
                    )
                }
                .build()
            ProxyController
                .getInstance()
                .setProxyOverride(proxyConfig, PROXY_CHANGE_EXECUTOR, PROXY_CHANGE_LISTENER)
        }
    }
}
