package com.iurysza.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.iurysza.CustomWebViewClient
import com.iurysza.R
import com.iurysza.WebviewPackageName
import com.iurysza.proxy.CustomProxyController

class MainActivity : AppCompatActivity() {
    private val rootView: LinearLayout by lazy { findViewById(R.id.activityRoot) }
    private val urlInputText by lazy { findViewById<TextInputEditText>(R.id.textUrl) }
    private val loadUrlBtn by lazy { findViewById<Button>(R.id.loadUrlButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadUrlBtn.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
//            Log.e(TAG, urlInputText.text.toString())
//            val url = urlInputText.text.toString()
//            webView.loadUrl("""https://$url""")
        }

        if (!WebviewPackageName.isRandom) return

        val webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootView.addView(webView)
        webView.setupWebview()
        webView.loadUrl("https://www.google.com")

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

val TAG = "PROXY_TEST"
