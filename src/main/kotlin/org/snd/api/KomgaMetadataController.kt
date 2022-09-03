package org.snd.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.Context
import io.javalin.http.HttpCode.*
import org.snd.komga.KomgaMetadataService
import org.snd.komga.model.dto.KomgaLibraryId
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesSearchResult
import java.util.concurrent.ExecutorService

class KomgaMetadataController(
    private val komgaMetadataService: KomgaMetadataService,
    private val taskHandler: ExecutorService,
    private val moshi: Moshi,
) {

    fun register() {
        path("/") {
            get("providers", this::providers)
            get("search", this::searchSeries)
            post("identify", this::identifySeries)
            post("match/series/{id}", this::matchSeries)
            post("match/library/{id}", this::matchLibrary)
            post("reset/series/{id}", this::resetSeries)
            post("reset/library/{id}", this::resetLibrary)
        }
    }

    private fun searchSeries(ctx: Context): Context {
        val seriesName = ctx.queryParam("name") ?: return ctx.status(BAD_REQUEST)
        val searchResults = komgaMetadataService.searchSeriesMetadata(seriesName)

        return ctx.result(moshi.adapter<Collection<SeriesSearchResult>>().toJson(searchResults))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }

    private fun identifySeries(ctx: Context): Context {
        val request = moshi.adapter<IdentifySeriesRequest>().fromJson(ctx.body()) ?: return ctx.status(BAD_REQUEST)
        komgaMetadataService.setSeriesMetadata(
            KomgaSeriesId(request.seriesId),
            Provider.valueOf(request.provider.uppercase()),
            ProviderSeriesId(request.providerSeriesId),
            request.edition
        )

        return ctx.status(NO_CONTENT)
    }

    private fun matchSeries(ctx: Context): Context {
        val seriesId = KomgaSeriesId(ctx.pathParam("id"))
        komgaMetadataService.matchSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    private fun matchLibrary(ctx: Context): Context {
        val libraryId = KomgaLibraryId(ctx.pathParam("id"))

        taskHandler.submit {
            try {
                komgaMetadataService.matchLibraryMetadata(libraryId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ctx.status(ACCEPTED)
    }

    private fun resetSeries(ctx: Context): Context {
        val seriesId = KomgaSeriesId(ctx.pathParam("id"))
        komgaMetadataService.resetSeriesMetadata(seriesId)
        return ctx.status(NO_CONTENT)
    }

    private fun resetLibrary(ctx: Context): Context {
        val libraryId = KomgaLibraryId(ctx.pathParam("id"))
        komgaMetadataService.resetLibraryMetadata(libraryId)
        return ctx.status(NO_CONTENT)
    }

    private fun providers(ctx: Context): Context {
        val providers = komgaMetadataService.availableProviders().map { it.name }
        return ctx.result(moshi.adapter<Collection<String>>().toJson(providers))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }
}
