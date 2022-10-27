package org.snd.infra

class HttpResponse(
    val body: ByteArray?,
    val code: Int,
    val headers: Map<String, String>
)