package snd.komf.app.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.http.contentType
import io.ktor.http.parseUrl
import io.ktor.server.plugins.cachingheaders.caching
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import snd.komf.api.KomfErrorResponse
import snd.komf.api.mangabaka.KomfMangaBakaLinkRequest
import snd.komf.api.mangabaka.KomfMangaBakaUnlinkRequest
import snd.komf.app.api.mappers.toDto
import snd.komf.mangabaka.model.MangaBakaSeriesId
import snd.komf.mangabaka.repository.MangaBakaRepository
import snd.komf.model.KomgaSeriesId

class MangaBakaRoutes(
    private val mangaBakaRepository: Flow<MangaBakaRepository>,
    private val httpClient: Flow<HttpClient>,
) {
    fun registerRoutes(routing: Route) {
        routing.route("/mangabaka") {
            get("/gstatic-favicon") { getFavIcon() }
            route("/series") {
                get("/{seriesId}/cover") { getCover() }
                get("/tags") { getTags() }

                route("/linked") {
                    get("/{seriesId}") { getLinked() }
                    post("/batch") { batch() }
                }

                route("/link") {
                    post { link() }
                    delete { unlink() }
                    get("/search") { search() }
                    post("/match") { match() }
                }
            }
        }
    }

    private suspend fun RoutingContext.getLinked() {
        val seriesId = KomgaSeriesId(call.parameters.getOrFail("seriesId"))
        val mangaBakaRepository = mangaBakaRepository.first()
        val series = mangaBakaRepository.find(seriesId)

        if (series == null) {
            call.respond(
                HttpStatusCode.NotFound,
                KomfErrorResponse("series is not matched with MangaBaka")
            )
            return
        }

        call.respond(HttpStatusCode.OK, series.toDto())
    }

    private suspend fun RoutingContext.batch() {
        val params = call.receive<List<String>>()
        val ids = params.map { KomgaSeriesId(it) }
        val series = mangaBakaRepository.first().findAllLinked(ids)
        call.respond(HttpStatusCode.OK, series.map { it.toDto() })
    }

    private suspend fun RoutingContext.link() {
        val request = call.receive<KomfMangaBakaLinkRequest>()
        mangaBakaRepository.first().link(
            KomgaSeriesId(request.komgaId.value),
            MangaBakaSeriesId(request.mangaBakaSeriesId.value)
        )
        call.respond(HttpStatusCode.OK)
    }

    private suspend fun RoutingContext.unlink() {
        val request = call.receive<KomfMangaBakaUnlinkRequest>()
        mangaBakaRepository.first().unlink(KomgaSeriesId(request.komgaId.value))
        call.respond(HttpStatusCode.OK)
    }

    private suspend fun RoutingContext.search() {
        val title = call.request.queryParameters.getOrFail("title")
        val series = mangaBakaRepository.first().search(title)
        call.respond(HttpStatusCode.OK, series.map { it.toDto() })
    }

    private suspend fun RoutingContext.match() {
        call.respond(HttpStatusCode.NotImplemented)
    }

    private suspend fun RoutingContext.getCover() {
        val seriesId = MangaBakaSeriesId(call.parameters.getOrFail("seriesId").toLong())
        val series = mangaBakaRepository.first().get(seriesId)
        val coverLink = series.cover.x350?.x1
        if (coverLink == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }
        val response = httpClient.first().get(coverLink)
        call.respondBytes(
            bytes = response.bodyAsBytes(),
            contentType = response.contentType(),
            status = HttpStatusCode.OK,
        )
    }

    private suspend fun RoutingContext.getTags() {
        val tags = mangaBakaRepository.first().getAllTags()
        call.caching = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 900))
        call.respond(HttpStatusCode.OK, tags.map { it.toDto() })
    }

    private suspend fun RoutingContext.getFavIcon() {
        val urlString = call.queryParameters.getOrFail("url")
        val url = parseUrl(urlString)
        if (url == null) {
            call.respond(HttpStatusCode.BadRequest)
            return
        }

        val response = httpClient.first().get("https://t1.gstatic.com/faviconV2") {
            parameter("client", "SOCIAL")
            parameter("type", "FAVICON")
            parameter("fallback_opts", "TYPE,SIZE,URL")
            parameter("url", "${url.protocol.name}://${url.host}")
            parameter("size", "48")
        }
        if (response.status == HttpStatusCode.NotFound) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        response.headers[HttpHeaders.ContentType]?.let {
            call.response.headers.append(HttpHeaders.ContentType, it)
        }
        response.headers[HttpHeaders.Expires]?.let {
            call.response.headers.append(HttpHeaders.Expires, it)
        }
        response.headers[HttpHeaders.CacheControl]?.let {
            call.response.headers.append(HttpHeaders.CacheControl, it)
        }
        response.headers[HttpHeaders.LastModified]?.let {
            call.response.headers.append(HttpHeaders.LastModified, it)
        }
        call.respond(
            HttpStatusCode.OK,
            response.bodyAsBytes()
        )
    }
}
