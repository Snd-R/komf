package org.snd.module

import com.squareup.moshi.Moshi
import org.snd.common.json.BigDecimalAdapter
import org.snd.common.json.LocalDateAdapter
import org.snd.common.json.LocalDateTimeAdapter
import org.snd.common.json.UriAdapter
import org.snd.common.json.ZonedDateTimeAdapter
import org.snd.mediaserver.kavita.model.KavitaAgeRatingAdapter
import org.snd.mediaserver.kavita.model.KavitaPersonRoleAdapter
import org.snd.mediaserver.kavita.model.KavitaPublicationStatusAdapter
import org.snd.mediaserver.komga.model.dto.KomgaReadingDirectionAdapter
import org.snd.metadata.model.metadata.json.ProviderBookIdJsonAdapter
import org.snd.metadata.model.metadata.json.ProviderSeriesIdJsonAdapter
import org.snd.metadata.providers.bangumi.model.json.InfoValueAdapter
import org.snd.metadata.providers.bangumi.model.json.PersonTypeAdapter
import org.snd.metadata.providers.bangumi.model.json.SubjectTypeAdapter
import org.snd.metadata.providers.mal.model.json.SearchResultsJsonAdapter
import org.snd.metadata.providers.mal.model.json.SeriesJsonAdapter
import org.snd.metadata.providers.mangadex.model.json.MangaDexCoverArtJsonAdapter
import org.snd.metadata.providers.mangadex.model.json.MangaDexMangaJsonAdapter
import org.snd.metadata.providers.mangaupdates.model.json.MangaUpdatesSearchResultsJsonAdapter
import org.snd.metadata.providers.mangaupdates.model.json.MangaUpdatesSeriesJsonAdapter
import org.snd.metadata.providers.yenpress.model.YenPressSearchResultJsonAdapter

class JsonModule {
    val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdapter())
        .add(LocalDateAdapter())
        .add(ZonedDateTimeAdapter())
        .add(LocalDateTimeAdapter())
        .add(UriAdapter())
        .add(SearchResultsJsonAdapter())
        .add(SeriesJsonAdapter())
        .add(MangaUpdatesSearchResultsJsonAdapter())
        .add(MangaUpdatesSeriesJsonAdapter())
        .add(KomgaReadingDirectionAdapter())
        .add(KavitaPublicationStatusAdapter())
        .add(KavitaAgeRatingAdapter())
        .add(KavitaPersonRoleAdapter())
        .add(MangaDexMangaJsonAdapter())
        .add(MangaDexCoverArtJsonAdapter())
        .add(ProviderSeriesIdJsonAdapter())
        .add(ProviderBookIdJsonAdapter())
        .add(YenPressSearchResultJsonAdapter())
        .add(SubjectTypeAdapter())
        .add(InfoValueAdapter())
        .add(PersonTypeAdapter())
        .build()
}
