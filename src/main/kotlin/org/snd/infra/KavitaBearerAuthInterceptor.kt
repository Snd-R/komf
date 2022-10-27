package org.snd.infra

import okhttp3.Interceptor
import okhttp3.Response
import org.snd.mediaserver.kavita.KavitaTokenProvider

class KavitaBearerAuthInterceptor(private val tokenProvider: KavitaTokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder().header("Authorization", "Bearer ${tokenProvider.getToken()}")
        return chain.proceed(builder.build())
    }
}

