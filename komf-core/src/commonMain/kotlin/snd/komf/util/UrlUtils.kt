package snd.komf.util

import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.authority
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPath
import io.ktor.http.encodeURLQueryComponent

// if Url is constructed with parseUrl function the resulting inner fields are not actually url encoded
// use this function to get actual url encoded path and parameters
// code is adapted from io.ktor.http.URLBuilder with added url encoding
fun Url.toStingEncoded(): String {
    val url = URLBuilder(this)
    val out = StringBuilder(256)
    out.append(url.protocol.name)

    when (protocol.name) {
        "file" -> {
            out.appendFile(host, encodedPath)
            return out.toString()
        }

        "mailto" -> {
            out.appendMailto(buildString { appendUserAndPassword(encodedUser, encodedPassword) }, host)
            return out.toString()
        }

        "about" -> {
            out.appendPayload(host)
            return out.toString()
        }

        "tel" -> {
            out.appendPayload(host)
            return out.toString()
        }

        "data" -> {
            out.appendPayload(host)
            return out.toString()
        }
    }


    out.append("://")
    out.append(url.authority)
    val encodedPath = url.encodedPathSegments.joinPathAndEncode()
    out.appendUrlFullPath(
        encodedPath,
        url.encodedParameters,
        url.trailingQuery
    )

    if (encodedFragment.isNotEmpty()) {
        out.append('#')
        out.append(encodedFragment.encodeURLParameter())
    }

    return out.toString()
}

private fun List<String>.joinPathAndEncode(): String {
    if (isEmpty()) return ""
    if (size == 1) {
        if (first().isEmpty()) return "/"
        return first()
    }

    return joinToString("/") { it.encodeURLPath(encodeSlash = true, encodeEncoded = false) }
}

private fun Appendable.appendUrlFullPath(
    encodedPath: String,
    encodedQueryParameters: ParametersBuilder,
    trailingQuery: Boolean
) {
    if (encodedPath.isNotBlank() && !encodedPath.startsWith("/")) {
        append('/')
    }

    append(encodedPath)

    if (!encodedQueryParameters.isEmpty() || trailingQuery) {
        append("?")
    }

    encodedQueryParameters.entries()
        .flatMap { (key, value) ->
            if (value.isEmpty()) listOf(key to null) else value.map { key to it }
        }
        .joinTo(this, "&") {
            val key = it.first
            if (it.second == null) {
                key.encodeURLParameter()
            } else {
                val value = it.second.toString()
                "${key.encodeURLParameter()}=${value.encodeURLQueryComponent()}"
            }
        }
}

private fun Appendable.appendMailto(encodedUser: String, host: String) {
    append(":")
    append(encodedUser)
    append(host)
}

private fun Appendable.appendFile(host: String, encodedPath: String) {
    append("://")
    append(host)
    if (!encodedPath.startsWith('/')) {
        append('/')
    }
    append(encodedPath)
}

private fun Appendable.appendPayload(host: String) {
    append(":")
    append(host)
}

private fun StringBuilder.appendUserAndPassword(encodedUser: String?, encodedPassword: String?) {
    if (encodedUser == null) {
        return
    }
    append(encodedUser)

    if (encodedPassword != null) {
        append(':')
        append(encodedPassword)
    }

    append("@")
}
