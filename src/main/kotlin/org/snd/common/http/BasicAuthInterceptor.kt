package org.snd.common.http

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(id: String, password: String) : Interceptor {
    var credentials: String
    init {
        credentials = Credentials.basic(id, password)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder().header("Authorization", credentials)
        return chain.proceed(builder.build())
    }
}

