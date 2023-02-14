package org.snd.common.http

class HttpResponse(
    val body: ByteArray?,
    val code: Int,
    val headers: Map<String, String>
)