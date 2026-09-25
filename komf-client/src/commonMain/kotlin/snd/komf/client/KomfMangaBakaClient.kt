package snd.komf.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import snd.komf.api.KomfServerSeriesId
import snd.komf.api.mangabaka.KomfMangaBakaLinkRequest
import snd.komf.api.mangabaka.KomfMangaBakaLinkedSeries
import snd.komf.api.mangabaka.KomfMangaBakaSeries
import snd.komf.api.mangabaka.KomfMangaBakaTag
import snd.komf.api.mangabaka.KomfMangaBakaUnlinkRequest
import snd.komf.api.mangabaka.MangaBakaSeriesId

class KomfMangaBakaClient(
    private val ktor: HttpClient,
) {
    private val metadataApiPrefix = "api/mangabaka"

    suspend fun getLinked(id: KomfServerSeriesId): KomfMangaBakaLinkedSeries? {
        return try {
            ktor.get("$metadataApiPrefix/series/linked/$id").body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) return null
            else throw e
        }
    }

    suspend fun getAllLinked(ids: List<KomfServerSeriesId>): List<KomfMangaBakaLinkedSeries> {
        return ktor.post("$metadataApiPrefix/series/linked/batch") {
            contentType(ContentType.Application.Json)
            setBody(ids)
        }.body()
    }

    suspend fun link(id: KomfServerSeriesId, mangaBakaId: MangaBakaSeriesId) {
        return ktor.post("$metadataApiPrefix/series/link") {
            contentType(ContentType.Application.Json)
            setBody(KomfMangaBakaLinkRequest(id, mangaBakaId))
        }.body()
    }

    suspend fun unlink(id: KomfServerSeriesId) {
        return ktor.delete("$metadataApiPrefix/series/link") {
            contentType(ContentType.Application.Json)
            setBody(KomfMangaBakaUnlinkRequest(id))
        }.body()
    }

    suspend fun search(title: String): List<KomfMangaBakaSeries> {
        return ktor.get("$metadataApiPrefix/series/link/search") {
            parameter("title", title)
        }.body()
    }

    suspend fun match(id: KomfServerSeriesId) {
        return ktor.delete("$metadataApiPrefix/series/link/match") {
            contentType(ContentType.Application.Json)
            setBody(KomfMangaBakaUnlinkRequest(id))
        }.body()
    }

    suspend fun getTags(): List<KomfMangaBakaTag> {
        return ktor.get("$metadataApiPrefix/series/tags").body()
    }

    suspend fun getFavicon(url: String): ByteArray {
        return ktor.get("$metadataApiPrefix/gstatic-favicon") {
            parameter("url", url)
        }.body()
    }

    suspend fun getCover(id: MangaBakaSeriesId): ByteArray {
        return ktor.get("$metadataApiPrefix/series/$id/cover").body()
    }
}