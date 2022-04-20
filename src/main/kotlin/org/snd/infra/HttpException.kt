package org.snd.infra

class HttpException(val code: Int, val headers: Map<String, String>, message: String) : RuntimeException(message)
