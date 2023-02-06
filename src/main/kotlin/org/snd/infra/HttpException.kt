package org.snd.infra

import okhttp3.Response

open class HttpException : RuntimeException {
    val code: Int
    val headers: Map<String, String>
    val url: String
    val body: String?
    final override val message: String

    constructor(
        code: Int,
        headers: Map<String, String>,
        url: String,
        body: String?,
    ) : super() {
        this.code = code
        this.headers = headers
        this.url = url
        this.body = body
        this.message = "response code: $code url: $url body: $body"
    }

    constructor(response: Response) : this(
        response.code,
        response.headers.toMap(),
        response.request.url.toString(),
        response.body?.string()
    )


    class NotFound(headers: Map<String, String>, url: String, body: String?) : HttpException(404, headers, url, body) {
        constructor(response: Response) : this(
            response.headers.toMap(),
            response.request.url.toString(),
            response.body?.string()
        )
    }

}
