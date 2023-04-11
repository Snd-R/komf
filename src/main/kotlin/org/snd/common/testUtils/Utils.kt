package org.snd.common.testUtils

import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.snd.common.http.HttpClient

object Utils {
    fun createHttpClient(name: String): HttpClient {
        val okHttpClient = OkHttpClient.Builder().build()
        val builder = okHttpClient.newBuilder()
            .addInterceptor(HttpLoggingInterceptor { message ->
                KotlinLogging.logger {}.debug { message }
            }.setLevel(HttpLoggingInterceptor.Level.BASIC))
        return HttpClient(builder.build(), name)
    }
}