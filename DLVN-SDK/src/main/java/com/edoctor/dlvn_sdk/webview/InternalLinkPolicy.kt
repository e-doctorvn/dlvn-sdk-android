package com.edoctor.dlvn_sdk.webview

import com.edoctor.dlvn_sdk.Constants
import java.net.URI
import java.net.URISyntaxException

internal fun isInternalDaiichiUrl(url: String?): Boolean {
    val uri = try {
        URI(url ?: return false)
    } catch (_: URISyntaxException) {
        return false
    }
    if (!uri.scheme.equals("https", ignoreCase = true) &&
        !uri.scheme.equals("http", ignoreCase = true)) return false
    val host = uri.host ?: return false
    return host.equals(Constants.dlvnDomain, ignoreCase = true) ||
        host.endsWith(".${Constants.dlvnDomain}", ignoreCase = true)
}
