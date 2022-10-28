package org.snd.infra

import okhttp3.Response

open class HttpException(val code: Int, val headers: Map<String, String>, message: String) : RuntimeException(message) {
    constructor(response: Response) : this(response.code, response.headers.toMap(), responseMessage(response))

    class NotFound(code: Int, headers: Map<String, String>, message: String) : HttpException(code, headers, message) {
        constructor(response: Response) : this(response.code, response.headers.toMap(), responseMessage(response))
    }
}

fun responseMessage(response: Response): String {
    return "response code: ${response.code} url: ${response.request.url} body: ${response.body?.string()}"
}
