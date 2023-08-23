package org.snd.common.http

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder().header("User-Agent", "snd-r/komf (https://github.com/Snd-R/komf)")
        return chain.proceed(builder.build())
    }
}
