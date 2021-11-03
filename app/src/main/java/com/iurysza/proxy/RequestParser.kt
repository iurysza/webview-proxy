package com.iurysza.proxy

import java.io.InputStream


object RequestParser {
    data class RequestLine(var action: String?, var uri: String?)

    fun parseRequestLine(inputStream: InputStream): RequestLine? {
        val requestLine: String = inputStream.bufferedReader().readLine()
        val requestLineItems: Array<String> = splitStringOnWhitespace(requestLine)
        return parseRequestItems(requestLineItems)
    }

    private fun splitStringOnWhitespace(requestLine: String): Array<String> {
        return requestLine.trim { it <= ' ' }.split("\\s+".toRegex()).toTypedArray()
    }

    private fun parseRequestItems(requestLineItems: Array<String>?): RequestLine? {
        requestLineItems?.let {
            val requestUriItems: Array<String>? = parseRequestUri(it[1])
            return RequestLine(it[0], requestUriItems?.get(0))
        }
        return null
    }

    private fun parseRequestUri(requestUri: String?): Array<String>? {
        requestUri?.let {
            val requestUriItems = arrayOf(requestUri, "")
            val questionMarkIndex = requestUri.indexOf("?")
            if (questionMarkIndex != -1) {
                requestUriItems[0] = requestUri.substring(0, questionMarkIndex)
                requestUriItems[1] = requestUri.substring(questionMarkIndex + 1)
            }
            return requestUriItems
        }
        return null
    }
}
