package org.snd.metadata.anilist

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import io.github.resilience4j.kotlin.ratelimiter.rateLimiter
import io.github.resilience4j.kotlin.retry.retry
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.snd.MediaQuery
import org.snd.SearchQuery
import org.snd.fragment.AniListManga
import org.snd.infra.HttpException
import org.snd.metadata.model.Thumbnail
import java.io.IOException
import java.util.function.Supplier

class AniListClient(
    private val okHttpClient: OkHttpClient,
    name: String,
    rateLimiterConfig: RateLimiterConfig = RateLimiterConfig.ofDefaults(),
    retryConfig: RetryConfig = RetryConfig.ofDefaults()
) {
    private val rateLimit = RateLimiter.of(name, rateLimiterConfig)
    private val retry = Retry.of(name, retryConfig)
    private val apolloClient = ApolloClient.Builder()
        .serverUrl("https://graphql.anilist.co")
        .okHttpClient(okHttpClient)
        .build()

    fun search(name: String, pageSize: Int = 10): List<SearchQuery.Medium> {
        return runBlocking {
            apolloClient.query(SearchQuery(search = name, pageSize))
                .toFlow()
                .retry(retry)
                .rateLimiter(rateLimit)
                .single()
                .data?.mediaSearch?.media ?: emptyList()
        }
    }

    fun getMedia(id: Int): MediaQuery.Series {
        return runBlocking {
            apolloClient.query(MediaQuery(id = id))
                .toFlow()
                .retry(retry)
                .rateLimiter(rateLimit)
                .single()
                .data?.series!!
        }
    }

    fun getThumbnail(series: AniListManga): Thumbnail? {
        return rateLimited {
            series.coverImage?.large?.toHttpUrlOrNull()?.let {
                val request = Request.Builder().url(it).build()
                val bytes = okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw HttpException(response)
                    response.body?.bytes() ?: throw IOException("empty body")
                }

                Thumbnail(bytes)
            }
        }
    }

    private fun <T> rateLimited(supplier: Supplier<T>): T {
        val rateLimited = RateLimiter.decorateSupplier(rateLimit, supplier)
        val retryable = Retry.decorateSupplier(retry, rateLimited)
        return retryable.get()
    }
}
