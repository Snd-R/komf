package org.snd.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.Context
import io.javalin.http.HttpStatus.*
import org.snd.mediaserver.MetadataService
import org.snd.mediaserver.MetadataUpdateService
import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServer.KOMGA
import org.snd.mediaserver.model.MediaServerLibraryId
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesSearchResult
import java.util.concurrent.ExecutorService

class MetadataController(
    private val metadataService: MetadataService,
    private val metadataUpdateService: MetadataUpdateService,
    private val taskHandler: ExecutorService,
    private val moshi: Moshi,
    private val serverType: MediaServer,
) {

    fun register() {
        path("/") {
            //deprecated endpoints
            if (serverType == KOMGA) {
                get("providers", this::providers)
                get("search", this::searchSeries)
                post("identify", this::identifySeries)
                post("match/series/{id}", this::matchSeries)
                post("match/library/{id}", this::matchLibrary)
                post("reset/series/{id}", this::resetSeries)
                post("reset/library/{id}", this::resetLibrary)
            }

            path(serverType.name.lowercase()) {
                get("providers", this::providers)
                get("search", this::searchSeries)
                post("identify", this::identifySeries)
                post("match/series/{id}", this::matchSeries)
                post("match/library/{id}", this::matchLibrary)
                post("reset/series/{id}", this::resetSeries)
                post("reset/library/{id}", this::resetLibrary)
            }
        }
    }

    private fun searchSeries(ctx: Context): Context {
        val seriesName = ctx.queryParam("name") ?: return ctx.status(BAD_REQUEST)
        val searchResults = metadataService.searchSeriesMetadata(seriesName)

        return ctx.result(moshi.adapter<Collection<SeriesSearchResult>>().toJson(searchResults))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }

    private fun identifySeries(ctx: Context): Context {
        val request = moshi.adapter<IdentifySeriesRequest>().fromJson(ctx.body()) ?: return ctx.status(BAD_REQUEST)
        metadataService.setSeriesMetadata(
            MediaServerSeriesId(request.seriesId),
            Provider.valueOf(request.provider.uppercase()),
            ProviderSeriesId(request.providerSeriesId),
            request.edition
        )

        return ctx.status(NO_CONTENT)
    }

    private fun matchSeries(ctx: Context): Context {
        val seriesId = MediaServerSeriesId(ctx.pathParam("id"))
        metadataService.matchSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    private fun matchLibrary(ctx: Context): Context {
        val libraryId = MediaServerLibraryId(ctx.pathParam("id"))

        taskHandler.submit {
            try {
                metadataService.matchLibraryMetadata(libraryId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ctx.status(ACCEPTED)
    }

    private fun resetSeries(ctx: Context): Context {
        val seriesId = MediaServerSeriesId(ctx.pathParam("id"))
        metadataUpdateService.resetSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    private fun resetLibrary(ctx: Context): Context {
        val libraryId = MediaServerLibraryId(ctx.pathParam("id"))
        metadataUpdateService.resetLibraryMetadata(libraryId)
        return ctx.status(NO_CONTENT)
    }

    private fun providers(ctx: Context): Context {
        val providers = metadataService.availableProviders().map { it.name }
        return ctx.result(moshi.adapter<Collection<String>>().toJson(providers))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }
}
