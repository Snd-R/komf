package snd.komf.app.api.mappers

import snd.komf.api.KomfServerSeriesId
import snd.komf.api.mangabaka.KomfMangaBakaLinkedSeries
import snd.komf.api.mangabaka.KomfMangaBakaSeries
import snd.komf.api.mangabaka.KomfMangaBakaTag
import snd.komf.api.mangabaka.MangaBakaAniListSource
import snd.komf.api.mangabaka.MangaBakaAnimeInfo
import snd.komf.api.mangabaka.MangaBakaAnimeNewsNetworkSource
import snd.komf.api.mangabaka.MangaBakaAnimePlanetSource
import snd.komf.api.mangabaka.MangaBakaContentRating
import snd.komf.api.mangabaka.MangaBakaCover
import snd.komf.api.mangabaka.MangaBakaCoverDpi
import snd.komf.api.mangabaka.MangaBakaCoverRaw
import snd.komf.api.mangabaka.MangaBakaKitsuSource
import snd.komf.api.mangabaka.MangaBakaLink
import snd.komf.api.mangabaka.MangaBakaLinkId
import snd.komf.api.mangabaka.MangaBakaLinkType
import snd.komf.api.mangabaka.MangaBakaMangaUpdatesSource
import snd.komf.api.mangabaka.MangaBakaMyAnimeListSource
import snd.komf.api.mangabaka.MangaBakaPublishedDate
import snd.komf.api.mangabaka.MangaBakaPublisher
import snd.komf.api.mangabaka.MangaBakaRelationType
import snd.komf.api.mangabaka.MangaBakaRelationship
import snd.komf.api.mangabaka.MangaBakaRelationshipChronology
import snd.komf.api.mangabaka.MangaBakaRelationshipId
import snd.komf.api.mangabaka.MangaBakaSeriesId
import snd.komf.api.mangabaka.MangaBakaSeriesState
import snd.komf.api.mangabaka.MangaBakaSeriesTag
import snd.komf.api.mangabaka.MangaBakaShikimoriSource
import snd.komf.api.mangabaka.MangaBakaSource
import snd.komf.api.mangabaka.MangaBakaStatus
import snd.komf.api.mangabaka.MangaBakaTagId
import snd.komf.api.mangabaka.MangaBakaTagWeight
import snd.komf.api.mangabaka.MangaBakaTitle
import snd.komf.api.mangabaka.MangaBakaTitleTrait
import snd.komf.api.mangabaka.MangaBakaType
import snd.komf.mangabaka.model.MangaBakaLinkedSeries
import snd.komf.mangabaka.model.MangaBakaSeries

fun MangaBakaLinkedSeries.toDto(): KomfMangaBakaLinkedSeries {
    return KomfMangaBakaLinkedSeries(KomfServerSeriesId(komgaId.value), mangaBaka.toDto())
}

fun MangaBakaSeries.toDto(): KomfMangaBakaSeries {
    return KomfMangaBakaSeries(
        id = MangaBakaSeriesId(this.id.value),
        hasAnime = this.hasAnime,
        anime = this.anime?.let { anime ->
            MangaBakaAnimeInfo(start = anime.start, end = anime.end)
        },
        artists = this.artists,
        authors = this.authors,
        canonicalUrl = this.canonicalUrl,
        contentRating = when (this.contentRating) {
            snd.komf.mangabaka.model.MangaBakaContentRating.SAFE -> MangaBakaContentRating.SAFE
            snd.komf.mangabaka.model.MangaBakaContentRating.SUGGESTIVE -> MangaBakaContentRating.SUGGESTIVE
            snd.komf.mangabaka.model.MangaBakaContentRating.EROTICA -> MangaBakaContentRating.EROTICA
            snd.komf.mangabaka.model.MangaBakaContentRating.PORNOGRAPHIC -> MangaBakaContentRating.PORNOGRAPHIC
        },
        cover = MangaBakaCover(
            raw = this.cover.raw?.let { raw ->
                MangaBakaCoverRaw(
                    url = raw.url,
                    size = raw.size,
                    height = raw.height,
                    width = raw.width,
                    blurhash = raw.blurhash,
                    thumbhash = raw.thumbhash,
                    format = raw.format
                )
            },
            x150 = this.cover.x150?.let { x150 ->
                MangaBakaCoverDpi(
                    x1 = x150.x1,
                    x2 = x150.x2,
                    x3 = x150.x3,
                )
            },
            x250 = this.cover.x250?.let { x250 ->
                MangaBakaCoverDpi(
                    x1 = x250.x1,
                    x2 = x250.x2,
                    x3 = x250.x3,
                )
            },
            x350 = this.cover.x350?.let { x350 ->
                MangaBakaCoverDpi(
                    x1 = x350.x1,
                    x2 = x350.x2,
                    x3 = x350.x3,
                )
            },
        ),
        description = this.description,
        finalVolume = this.finalVolume,
        isLicensed = this.isLicensed,
        lastUpdatedAt = this.lastUpdatedAt,
        mergedWith = this.mergedWith,
        originalLanguage = this.originalLanguage,
        publishers = this.publishers?.map { publisher ->
            MangaBakaPublisher(
                name = publisher.name,
                note = publisher.note,
                type = publisher.type
            )
        },
        rating = this.rating,
        state = when (this.state) {
            snd.komf.mangabaka.model.MangaBakaSeriesState.ACTIVE -> MangaBakaSeriesState.ACTIVE
            snd.komf.mangabaka.model.MangaBakaSeriesState.MERGED -> MangaBakaSeriesState.MERGED
            snd.komf.mangabaka.model.MangaBakaSeriesState.DELETED -> MangaBakaSeriesState.DELETED
        },
        status = when (this.status) {
            snd.komf.mangabaka.model.MangaBakaStatus.CANCELLED -> MangaBakaStatus.CANCELLED
            snd.komf.mangabaka.model.MangaBakaStatus.COMPLETED -> MangaBakaStatus.COMPLETED
            snd.komf.mangabaka.model.MangaBakaStatus.HIATUS -> MangaBakaStatus.HIATUS
            snd.komf.mangabaka.model.MangaBakaStatus.RELEASING -> MangaBakaStatus.RELEASING
            snd.komf.mangabaka.model.MangaBakaStatus.UPCOMING -> MangaBakaStatus.UPCOMING
            snd.komf.mangabaka.model.MangaBakaStatus.UNKNOWN -> MangaBakaStatus.UNKNOWN
        },
        totalChapters = this.totalChapters,
        type = when (this.type) {
            snd.komf.mangabaka.model.MangaBakaType.MANGA -> MangaBakaType.MANGA
            snd.komf.mangabaka.model.MangaBakaType.NOVEL -> MangaBakaType.NOVEL
            snd.komf.mangabaka.model.MangaBakaType.MANHWA -> MangaBakaType.MANHWA
            snd.komf.mangabaka.model.MangaBakaType.MANHUA -> MangaBakaType.MANHUA
            snd.komf.mangabaka.model.MangaBakaType.OEL -> MangaBakaType.OEL
            snd.komf.mangabaka.model.MangaBakaType.OTHER -> MangaBakaType.OTHER
        },
        links = this.linksV2?.map { link ->
            MangaBakaLink(
                id = MangaBakaLinkId(link.id.value),
                language = link.language,
                name = link.name,
                nameDisplay = link.nameDisplay,
                type = when (link.type) {
                    snd.komf.mangabaka.model.MangaBakaLinkType.RETAILER -> MangaBakaLinkType.RETAILER
                    snd.komf.mangabaka.model.MangaBakaLinkType.PUBLISHER -> MangaBakaLinkType.PUBLISHER
                    snd.komf.mangabaka.model.MangaBakaLinkType.WEBPLATFORM -> MangaBakaLinkType.WEBPLATFORM
                    snd.komf.mangabaka.model.MangaBakaLinkType.INFO -> MangaBakaLinkType.INFO
                    snd.komf.mangabaka.model.MangaBakaLinkType.SOCIAL -> MangaBakaLinkType.SOCIAL
                    snd.komf.mangabaka.model.MangaBakaLinkType.NEWS -> MangaBakaLinkType.NEWS
                    snd.komf.mangabaka.model.MangaBakaLinkType.PIRACY -> MangaBakaLinkType.PIRACY
                    snd.komf.mangabaka.model.MangaBakaLinkType.OTHER -> MangaBakaLinkType.OTHER
                },
                url = link.url
            )
        },
        published = this.published?.let { published ->
            MangaBakaPublishedDate(
                endDate = published.endDate,
                endDateIsEstimated = published.endDateIsEstimated,
                startDate = published.startDate,
                startDateIsEstimated = published.startDateIsEstimated
            )
        },
        relationships = this.relationshipsV2?.map { relationship ->
            MangaBakaRelationship(
                id = MangaBakaRelationshipId(relationship.id.value),
                chronology = when (relationship.chronology) {
                    snd.komf.mangabaka.model.MangaBakaRelationshipChronology.NARRATIVE -> MangaBakaRelationshipChronology.NARRATIVE
                    snd.komf.mangabaka.model.MangaBakaRelationshipChronology.RELEASE -> MangaBakaRelationshipChronology.RELEASE
                    snd.komf.mangabaka.model.MangaBakaRelationshipChronology.UNKNOWN -> MangaBakaRelationshipChronology.UNKNOWN
                },
                isManual = relationship.isManual,
                note = relationship.note,
                relationType = when (relationship.relationType) {
                    snd.komf.mangabaka.model.MangaBakaRelationType.ADAPTATION -> MangaBakaRelationType.ADAPTATION
                    snd.komf.mangabaka.model.MangaBakaRelationType.ALTERNATIVE -> MangaBakaRelationType.ALTERNATIVE
                    snd.komf.mangabaka.model.MangaBakaRelationType.CAMEO -> MangaBakaRelationType.CAMEO
                    snd.komf.mangabaka.model.MangaBakaRelationType.CHARACTER_FOCUS -> MangaBakaRelationType.CHARACTER_FOCUS
                    snd.komf.mangabaka.model.MangaBakaRelationType.COMPILATION -> MangaBakaRelationType.COMPILATION
                    snd.komf.mangabaka.model.MangaBakaRelationType.CONTAINS -> MangaBakaRelationType.CONTAINS
                    snd.komf.mangabaka.model.MangaBakaRelationType.CROSSOVER -> MangaBakaRelationType.CROSSOVER
                    snd.komf.mangabaka.model.MangaBakaRelationType.EXPANSION -> MangaBakaRelationType.EXPANSION
                    snd.komf.mangabaka.model.MangaBakaRelationType.MAIN -> MangaBakaRelationType.MAIN
                    snd.komf.mangabaka.model.MangaBakaRelationType.OTHER -> MangaBakaRelationType.OTHER
                    snd.komf.mangabaka.model.MangaBakaRelationType.PARENT -> MangaBakaRelationType.PARENT
                    snd.komf.mangabaka.model.MangaBakaRelationType.PARODY -> MangaBakaRelationType.PARODY
                    snd.komf.mangabaka.model.MangaBakaRelationType.PREQUEL -> MangaBakaRelationType.PREQUEL
                    snd.komf.mangabaka.model.MangaBakaRelationType.REBOOT -> MangaBakaRelationType.REBOOT
                    snd.komf.mangabaka.model.MangaBakaRelationType.REMAKE -> MangaBakaRelationType.REMAKE
                    snd.komf.mangabaka.model.MangaBakaRelationType.SEQUEL -> MangaBakaRelationType.SEQUEL
                    snd.komf.mangabaka.model.MangaBakaRelationType.SERIES -> MangaBakaRelationType.SERIES
                    snd.komf.mangabaka.model.MangaBakaRelationType.SIDE_STORY -> MangaBakaRelationType.SIDE_STORY
                    snd.komf.mangabaka.model.MangaBakaRelationType.SOURCE -> MangaBakaRelationType.SOURCE
                    snd.komf.mangabaka.model.MangaBakaRelationType.SPIN_OFF -> MangaBakaRelationType.SPIN_OFF
                    snd.komf.mangabaka.model.MangaBakaRelationType.SUMMARY -> MangaBakaRelationType.SUMMARY
                    snd.komf.mangabaka.model.MangaBakaRelationType.UNCOLLECTED -> MangaBakaRelationType.UNCOLLECTED
                },
                toSeriesId = MangaBakaSeriesId(relationship.toSeriesId.value)
            )
        },
        tags = this.tagsV2?.map { it.toDto() },
        titles = this.titles?.map { title ->
            MangaBakaTitle(
                language = title.language,
                title = title.title,
                traits = title.traits.map {
                    when (it) {
                        snd.komf.mangabaka.model.MangaBakaTitleTrait.OFFICIAL -> MangaBakaTitleTrait.OFFICIAL
                        snd.komf.mangabaka.model.MangaBakaTitleTrait.NATIVE -> MangaBakaTitleTrait.NATIVE
                        snd.komf.mangabaka.model.MangaBakaTitleTrait.ALTERNATIVE -> MangaBakaTitleTrait.ALTERNATIVE
                    }
                },
                isPrimary = title.isPrimary,
                note = title.note
            )
        },
        source = MangaBakaSource(
            anilist = MangaBakaAniListSource(
                id = this.source.anilist.id,
                rating = this.source.anilist.rating,
                ratingNormalized = this.source.anilist.ratingNormalized,
            ),
            animeNewsNetwork = MangaBakaAnimeNewsNetworkSource(
                id = this.source.animeNewsNetwork.id,
                rating = this.source.animeNewsNetwork.rating,
                ratingNormalized = this.source.animeNewsNetwork.ratingNormalized,
            ),
            animePlanet = MangaBakaAnimePlanetSource(
                id = this.source.animePlanet.id,
                rating = this.source.animePlanet.rating,
                ratingNormalized = this.source.animePlanet.ratingNormalized,
            ),
            kitsu = MangaBakaKitsuSource(
                id = this.source.kitsu.id,
                rating = this.source.kitsu.rating,
                ratingNormalized = this.source.kitsu.ratingNormalized,
            ),
            mangaUpdates = MangaBakaMangaUpdatesSource(
                id = this.source.mangaUpdates.id,
                rating = this.source.mangaUpdates.rating,
                ratingNormalized = this.source.mangaUpdates.ratingNormalized,
            ),
            myAnimeList = MangaBakaMyAnimeListSource(
                id = this.source.myAnimeList.id,
                rating = this.source.myAnimeList.rating,
                ratingNormalized = this.source.myAnimeList.ratingNormalized,
            ),
            shikimori = MangaBakaShikimoriSource(
                id = this.source.shikimori.id,
                rating = this.source.shikimori.rating,
                ratingNormalized = this.source.shikimori.ratingNormalized,
            )
        )
    )

}

fun snd.komf.mangabaka.model.MangaBakaSeriesTag.toDto(): MangaBakaSeriesTag {
    return MangaBakaSeriesTag(
        id = MangaBakaTagId(this.id.value),
        contentRating = when (this.contentRating) {
            snd.komf.mangabaka.model.MangaBakaContentRating.SAFE -> MangaBakaContentRating.SAFE
            snd.komf.mangabaka.model.MangaBakaContentRating.SUGGESTIVE -> MangaBakaContentRating.SUGGESTIVE
            snd.komf.mangabaka.model.MangaBakaContentRating.EROTICA -> MangaBakaContentRating.EROTICA
            snd.komf.mangabaka.model.MangaBakaContentRating.PORNOGRAPHIC -> MangaBakaContentRating.PORNOGRAPHIC
        },
        description = this.description,
        isSpoiler = this.isSpoiler,
        level = this.level,
        name = this.name,
        namePath = this.namePath,
        parentId = this.parentId?.let { MangaBakaTagId(it.value) },
        seriesCount = this.seriesCount,
        impliedByTagIds = this.impliedByTagIds.map { MangaBakaTagId(it.value) },
        isExplicit = this.isExplicit,
        isGenre = this.isGenre,
        mergedWith = this.mergedWith,
        weight = when (this.weight) {
            snd.komf.mangabaka.model.MangaBakaTagWeight.CORE -> MangaBakaTagWeight.CORE
            snd.komf.mangabaka.model.MangaBakaTagWeight.DEFINING -> MangaBakaTagWeight.DEFINING
            snd.komf.mangabaka.model.MangaBakaTagWeight.RECURRENT -> MangaBakaTagWeight.RECURRENT
            snd.komf.mangabaka.model.MangaBakaTagWeight.INCIDENTAL -> MangaBakaTagWeight.INCIDENTAL
            snd.komf.mangabaka.model.MangaBakaTagWeight.UNWEIGHTED -> MangaBakaTagWeight.UNWEIGHTED
        },
    )
}

fun snd.komf.mangabaka.model.MangaBakaTag.toDto(): KomfMangaBakaTag {
    return KomfMangaBakaTag(
        id = MangaBakaTagId(this.id.value),
        contentRating = when (this.contentRating) {
            snd.komf.mangabaka.model.MangaBakaContentRating.SAFE -> MangaBakaContentRating.SAFE
            snd.komf.mangabaka.model.MangaBakaContentRating.SUGGESTIVE -> MangaBakaContentRating.SUGGESTIVE
            snd.komf.mangabaka.model.MangaBakaContentRating.EROTICA -> MangaBakaContentRating.EROTICA
            snd.komf.mangabaka.model.MangaBakaContentRating.PORNOGRAPHIC -> MangaBakaContentRating.PORNOGRAPHIC
        },
        description = this.description,
        isSpoiler = this.isSpoiler,
        level = this.level,
        name = this.name,
        namePath = this.namePath,
        parentId = this.parentId?.let { MangaBakaTagId(it.value) },
        seriesCount = this.seriesCount,
        isGenre = this.isGenre,
        mergedWith = this.mergedWith,
    )
}
