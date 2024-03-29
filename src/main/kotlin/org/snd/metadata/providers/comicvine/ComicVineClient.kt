package org.snd.metadata.providers.comicvine

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.snd.common.http.HttpClient
import org.snd.metadata.model.Image
import org.snd.metadata.providers.comicvine.ComicVineClient.ComicVineTypeId.ISSUE
import org.snd.metadata.providers.comicvine.ComicVineClient.ComicVineTypeId.VOLUME
import org.snd.metadata.providers.comicvine.model.ComicVineIssue
import org.snd.metadata.providers.comicvine.model.ComicVineIssueId
import org.snd.metadata.providers.comicvine.model.ComicVineSearchResult
import org.snd.metadata.providers.comicvine.model.ComicVineVolume
import org.snd.metadata.providers.comicvine.model.ComicVineVolumeId
import org.snd.metadata.providers.comicvine.model.ComicVineVolumeSearch

private val baseUrl: HttpUrl = "https://comicvine.gamespot.com/api/".toHttpUrl()

class ComicVineClient(
    private val client: HttpClient,
    private val moshi: Moshi,
) {

    fun searchVolume(name: String): ComicVineSearchResult<List<ComicVineVolumeSearch>> {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("search")
                .addQueryParameter("query", name)
                .addQueryParameter("format", "json")
                .addQueryParameter("resources", "volume")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getVolume(id: ComicVineVolumeId): ComicVineSearchResult<ComicVineVolume> {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("volume/${VOLUME.id}-${id.id}")
                .addQueryParameter("format", "json")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getIssue(id: ComicVineIssueId): ComicVineSearchResult<ComicVineIssue> {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("issue/${ISSUE.id}-${id.id}")
                .addQueryParameter("format", "json")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getCover(url: HttpUrl): Image {
        val request = Request.Builder().url(url).build()
        val bytes = client.executeWithByteResponse(request)
        return Image(bytes)
    }

    private inline fun <reified T : Any> parseJson(json: String): T {
        return moshi.adapter<T>().fromJson(json) ?: throw RuntimeException()
    }

    private enum class ComicVineTypeId(val id: Int) {
        VOLUME(4050),
        ISSUE(4000)
    }
}

