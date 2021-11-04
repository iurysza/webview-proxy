package com.iurysza.proxy

data class WebViewProxyConfig(
    val enabled: Boolean = true,
    val forHosts: List<String> = listOf("www.google.com"),
    val excludedHosts: List<String> = emptyList(),
    val queueSize: Int = 4
)
