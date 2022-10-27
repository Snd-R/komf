package org.snd.infra

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

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
                if (!response.isSuccessful) throw HttpException(response)
                response.body?.string() ?: ""
            }
        }
    }

    fun executeWithResponse(request: Request): HttpResponse {
        return rateLimited {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw HttpException(response)
                httpResponse(response)
            }
        }
    }

    fun executeWithByteResponse(request: Request): ByteArray {
        return rateLimited {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw HttpException(response)

                response.body?.bytes() ?: throw IOException("empty body")
            }
        }
    }

    private fun <T> rateLimited(supplier: () -> T): T {
        return RateLimiter.decorateCheckedSupplier(rateLimit, supplier)
            .let { Retry.decorateCheckedSupplier(retry, it) }
            .apply()
    }

    private fun httpResponse(response: Response) = HttpResponse(
        body = response.body?.bytes(),
        code = response.code,
        headers = response.headers.toMap()
    )
}
