package com.iurysza

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.iurysza.proxy.CustomProxyController

class CustomWebViewClient(private val proxyController: CustomProxyController) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        proxyController.applyProxyIfNeed(request)
//        Log.e(TAG, "shouldInterceptRequest: ${request?.url.toString()}")
        return super.shouldInterceptRequest(view, request)
    }
}
