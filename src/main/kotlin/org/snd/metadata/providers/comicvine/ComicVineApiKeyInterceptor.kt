package org.snd.metadata.providers.comicvine

import okhttp3.Interceptor
import okhttp3.Response

class ComicVineApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .url(request.url.newBuilder().addQueryParameter("api_key", apiKey).build())
            .build()
        return chain.proceed(newRequest)
    }
}