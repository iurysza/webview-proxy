package com.iurysza

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.iurysza.proxy.CustomProxyController

class CustomWebViewClient(private val proxyController: CustomProxyController) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        proxyController.startProxyIfNeeded(request?.url?.host)
        return super.shouldInterceptRequest(view, request)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        proxyController.checkSslError(error) {
            handler.proceed()
        }
    }

}


