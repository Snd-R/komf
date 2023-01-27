package org.snd.metadata.providers.anilist

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpNetworkTransport
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
import org.snd.metadata.model.Image
import org.snd.type.MediaFormat
import java.io.IOException

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
        .httpEngine(DefaultHttpEngine(okHttpClient))
        // avoids thread pool leak on client dispose by using HttpNetworkTransport instead of default WebSocketNetworkTransport that has backgroundDispatcher which is not getting closed
        // subscription is not used so this does not affect anything
        .subscriptionNetworkTransport(HttpNetworkTransport.Builder().serverUrl("https://graphql.anilist.co").build())
        .build()

    fun search(name: String, formats: List<MediaFormat>, pageSize: Int = 10): List<SearchQuery.Medium> {
        return runBlocking {
            apolloClient.query(
                SearchQuery(
                    search = name,
                    perPage = pageSize,
                    formats = formats
                )
            )
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

    fun getThumbnail(series: AniListManga): Image? {
        return rateLimited {
            series.coverImage?.large?.toHttpUrlOrNull()?.let {
                val request = Request.Builder().url(it).build()
                val bytes = okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw HttpException(response)
                    response.body?.bytes() ?: throw IOException("empty body")
                }

                Image(bytes)
            }
        }
    }

    private fun <T> rateLimited(supplier: () -> T): T {
        return RateLimiter.decorateCheckedSupplier(rateLimit, supplier)
            .let { Retry.decorateCheckedSupplier(retry, it) }
            .get()
    }

    fun close() {
        apolloClient.close()
    }
}
