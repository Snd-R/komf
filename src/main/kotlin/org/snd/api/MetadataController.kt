package org.snd.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.Context
import io.javalin.http.HttpStatus.ACCEPTED
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.CONFLICT
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.http.HttpStatus.OK
import org.snd.api.dto.IdentifySeriesRequest
import org.snd.mediaserver.MediaServerClient
import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerLibraryId
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesSearchResult
import org.snd.module.MediaServerModule.MetadataServiceProvider
import org.snd.module.MediaServerModule.MetadataUpdateServiceProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore

class MetadataController(
    private val metadataServiceProvider: MetadataServiceProvider,
    private val metadataUpdateServiceProvider: MetadataUpdateServiceProvider,
    private val mediaServerClient: MediaServerClient,
    private val taskHandler: ExecutorService,
    private val moshi: Moshi,
    private val serverType: MediaServer,
) {

    private val libraryScanSemaphore = Semaphore(1)

    fun register() {
        path("/") {
            path(serverType.name.lowercase()) {
                get("providers", this::providers)
                get("search", this::searchSeries)
                post("identify", this::identifySeries)

                post("match/library/{libraryId}/series/{seriesId}", this::matchLibrarySeries)
                post("match/library/{id}", this::matchLibrary)

                post("reset/library/{libraryId}/series/{seriesId}", this::resetLibrarySeries)
                post("reset/library/{id}", this::resetLibrary)

                post("match/series/{id}", this::matchSeries)
                post("reset/series/{id}", this::resetSeries)
            }
        }
    }

    private fun providers(ctx: Context): Context {
        val libraryId = ctx.queryParam("libraryId")?.let { MediaServerLibraryId(it) }

        val providers = (
                libraryId
                    ?.let { metadataServiceProvider.serviceFor(it.id).availableProviders(it) }
                    ?: metadataServiceProvider.default().availableProviders()
                )
            .map { it.providerName().name }

        return ctx.result(moshi.adapter<Collection<String>>().toJson(providers))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }

    private fun searchSeries(ctx: Context): Context {
        val seriesName = ctx.queryParam("name") ?: return ctx.status(BAD_REQUEST)
        val seriesId = ctx.queryParam("seriesId")?.let { MediaServerSeriesId(it) }

        val libraryId = ctx.queryParam("libraryId")?.let { MediaServerLibraryId(it) }
            ?: seriesId?.let { mediaServerClient.getSeries(it).libraryId }

        val searchResults = libraryId
            ?.let { metadataServiceProvider.serviceFor(it.id).searchSeriesMetadata(seriesName, it) }
            ?: metadataServiceProvider.default().searchSeriesMetadata(seriesName)

        return ctx.result(moshi.adapter<Collection<SeriesSearchResult>>().toJson(searchResults))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }

    private fun identifySeries(ctx: Context): Context {
        val request = moshi.adapter<IdentifySeriesRequest>().fromJson(ctx.body()) ?: return ctx.status(BAD_REQUEST)
        val libraryId = request.libraryId ?: mediaServerClient.getSeries(MediaServerSeriesId(request.seriesId)).libraryId.id
        metadataServiceProvider.serviceFor(libraryId).setSeriesMetadata(
            MediaServerSeriesId(request.seriesId),
            Provider.valueOf(request.provider.uppercase()),
            ProviderSeriesId(request.providerSeriesId),
            request.edition
        )

        return ctx.status(NO_CONTENT)
    }

    private fun matchLibrarySeries(ctx: Context): Context {
        val libraryId = ctx.pathParam("libraryId")
        val seriesId = MediaServerSeriesId(ctx.pathParam("seriesId"))
        metadataServiceProvider.serviceFor(libraryId).matchSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    @Deprecated("use matchLibrarySeries")
    private fun matchSeries(ctx: Context): Context {
        val seriesId = MediaServerSeriesId(ctx.pathParam("id"))
        metadataServiceProvider.default().matchSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    private fun matchLibrary(ctx: Context): Context {
        val libraryId = MediaServerLibraryId(ctx.pathParam("id"))
        return if (libraryScanSemaphore.tryAcquire()) {
            taskHandler.submit {
                try {
                    metadataServiceProvider.serviceFor(libraryId.id).matchLibraryMetadata(libraryId)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    libraryScanSemaphore.release()
                }
            }

            ctx.status(ACCEPTED)
        } else ctx.status(CONFLICT)
    }

    private fun resetLibrarySeries(ctx: Context): Context {
        val libraryId = ctx.pathParam("libraryId")
        val seriesId = MediaServerSeriesId(ctx.pathParam("seriesId"))
        metadataUpdateServiceProvider.serviceFor(libraryId).resetSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    @Deprecated("use resetLibrarySeries")
    private fun resetSeries(ctx: Context): Context {
        val seriesId = MediaServerSeriesId(ctx.pathParam("id"))
        metadataUpdateServiceProvider.default().resetSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    private fun resetLibrary(ctx: Context): Context {
        val libraryId = MediaServerLibraryId(ctx.pathParam("id"))
        metadataUpdateServiceProvider.serviceFor(libraryId.id).resetLibraryMetadata(libraryId)
        return ctx.status(NO_CONTENT)
    }
}
