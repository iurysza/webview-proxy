package com.iurysza.proxy.util


class ScriptFormatter() {
    companion object {
        const val SCRIPT_TAG = "<script>"
        const val SCRIPT_END_TAG = "</script>"
        const val DOC_LOADED_TEMPLATE = "window.addEventListener('DOMContentLoaded', function () { %s })"
    }

    fun getScriptsInHead(host: String): String {
        val scripts = StringBuilder()
        getScriptToBeInjected(host).forEach {
            if (!it.mainFrameOnly && it.injectionTime == ScriptInjectionTime.DOCUMENT_START) {
                scripts.append(SCRIPT_TAG)
                scripts.append(it.javaScript)
                scripts.append(SCRIPT_END_TAG)
            } else if (!it.mainFrameOnly && it.injectionTime == ScriptInjectionTime.DOCUMENT_END) {
                scripts.append(SCRIPT_TAG)
                scripts.append(DOC_LOADED_TEMPLATE.format(it.javaScript))
                scripts.append(SCRIPT_END_TAG)
            }
        }
        return scripts.toString()
    }

    private fun getScriptToBeInjected(host: String): List<Script> {
        return emptyList<Script>()
    }
}

enum class ScriptInjectionTime {
    DOCUMENT_START,
    DOCUMENT_END
}

data class Script(
    var id: String,
    var javaScript: String,
    var injectionTime: ScriptInjectionTime,
    var mainFrameOnly: Boolean
)
