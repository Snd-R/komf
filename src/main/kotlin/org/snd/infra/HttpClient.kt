package org.snd.infra

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.function.Supplier

val MEDIA_TYPE_JSON = "application/json".toMediaType()

open class HttpClient(
    private val client: OkHttpClient,
    name: String,
    rateLimiterConfig: RateLimiterConfig = RateLimiterConfig.ofDefaults(),
    retryConfig: RetryConfig = RetryConfig.ofDefaults()
) {
    private val rateLimit = RateLimiter.of(name, rateLimiterConfig)
    private val retry = Retry.of(name, retryConfig)

    fun execute(request: Request): String {
        return rateLimited {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                response.body?.string() ?: ""
            }
        }
    }

    fun executeAsync(request: Request) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { if (!response.isSuccessful) throw IOException("Unexpected code $response") }
            }
        })
    }

    fun executeWithByteResponse(request: Request): ByteArray {
        return rateLimited {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                response.body?.bytes() ?: throw IOException("empty body")
            }
        }
    }

    private fun <T> rateLimited(supplier: Supplier<T>): T {
        val rateLimited = RateLimiter.decorateSupplier(rateLimit, supplier)
        val retryable = Retry.decorateSupplier(retry, rateLimited)
        return retryable.get()
    }
}
