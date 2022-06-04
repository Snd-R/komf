package org.snd.module

import com.squareup.moshi.Moshi
import org.snd.infra.LocalDateAdapter
import org.snd.infra.LocalDateTimeAdapter
import org.snd.infra.UriAdapter
import org.snd.infra.ZonedDateTimeAdapter
import org.snd.metadata.mal.model.json.SearchResultsJsonAdapter
import org.snd.metadata.mal.model.json.SeriesJsonAdapter
import org.snd.metadata.mangaupdates.model.json.MangaUpdatesSearchResultsJsonAdapter
import org.snd.metadata.mangaupdates.model.json.MangaUpdatesSeriesJsonAdapter

class JsonModule {
    val moshi: Moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .add(ZonedDateTimeAdapter())
        .add(LocalDateTimeAdapter())
        .add(UriAdapter())
        .add(SearchResultsJsonAdapter())
        .add(SeriesJsonAdapter())
        .add(MangaUpdatesSearchResultsJsonAdapter())
        .add(MangaUpdatesSeriesJsonAdapter())
        .build()
}
