package org.snd.noifications.imgur

import okhttp3.Interceptor
import okhttp3.Response

class ClientIdAuthInterceptor(private val clientId: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder().header("Authorization", "Client-ID $clientId")
        return chain.proceed(builder.build())
    }
}