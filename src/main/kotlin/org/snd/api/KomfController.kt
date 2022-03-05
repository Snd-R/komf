package org.snd.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.Context
import io.javalin.http.HttpCode.*
import org.snd.komga.KomgaService
import org.snd.komga.model.LibraryId
import org.snd.komga.model.SeriesId
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.SeriesSearchResult
import java.util.concurrent.ExecutorService

class KomfController(
    private val komgaService: KomgaService,
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
        }
    }

    private fun searchSeries(ctx: Context): Context {
        val seriesName = ctx.queryParam("name") ?: return ctx.status(BAD_REQUEST)
        val searchResults = komgaService.searchSeriesMetadata(seriesName)

        return ctx.result(moshi.adapter<Collection<SeriesSearchResult>>().toJson(searchResults))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }

    private fun identifySeries(ctx: Context): Context {
        val request = moshi.adapter<IdentifySeriesRequest>().fromJson(ctx.body()) ?: return ctx.status(BAD_REQUEST)
        komgaService.setSeriesMetadata(
            SeriesId(request.seriesId),
            Provider.valueOf(request.provider.uppercase()),
            ProviderSeriesId(request.providerSeriesId)
        )

        return ctx.status(NO_CONTENT)
    }

    private fun matchSeries(ctx: Context): Context {
        val seriesId = SeriesId(ctx.pathParam("id"))
        val provider = ctx.queryParam("provider")?.let { Provider.valueOf(it.uppercase()) }
        komgaService.matchSeriesMetadata(seriesId, provider)
        return ctx.status(NO_CONTENT)
    }

    private fun matchLibrary(ctx: Context): Context {
        val libraryId = LibraryId(ctx.pathParam("id"))
        val provider = ctx.queryParam("provider")?.let { Provider.valueOf(it.uppercase()) }
        taskHandler.submit { komgaService.matchLibraryMetadata(libraryId, provider) }

        return ctx.status(ACCEPTED)
    }

    private fun providers(ctx: Context): Context {
        val providers = komgaService.availableProviders().map { it.name }
        return ctx.result(moshi.adapter<Collection<String>>().toJson(providers))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }
}
