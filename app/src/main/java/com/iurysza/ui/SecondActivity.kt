package com.iurysza.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.iurysza.CustomWebViewClient
import com.iurysza.R
import com.iurysza.WebviewPackageName
import com.iurysza.proxy.CustomProxyController

class SecondActivity : AppCompatActivity() {
    private val urlInputText by lazy { findViewById<TextInputEditText>(R.id.textUrl) }
    private val loadUrlBtn by lazy { findViewById<Button>(R.id.loadUrlButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity_main)
        val webView = findViewById<WebView>(R.id.webview).setupWebview()
        webView.loadUrl("https://www.google.com")

        loadUrlBtn.setOnClickListener {
            Log.e(TAG, urlInputText.text.toString())
            val url = urlInputText.text.toString()
//            webView.loadUrl("""https://$url""")
            WebviewPackageName.isRandom = true
            startActivity(Intent(this, MainActivity::class.java))
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.setupWebview(): WebView = apply {
        val proxyController = CustomProxyController()
        webViewClient = CustomWebViewClient(proxyController)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
    }
}
