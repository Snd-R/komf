package org.snd.metadata.providers.bookwalker

import okhttp3.Interceptor
import okhttp3.Response

class BookWalkerSafeSearchInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder().header("Cookie", "safeSearch=111; glSafeSearch=1")
        return chain.proceed(builder.build())
    }
}