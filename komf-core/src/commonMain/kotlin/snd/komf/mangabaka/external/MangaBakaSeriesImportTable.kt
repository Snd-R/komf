package snd.komf.mangabaka.external

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json
import snd.komf.mangabaka.model.MangaBakaLink
import snd.komf.mangabaka.model.MangaBakaPublisher
import snd.komf.mangabaka.model.MangaBakaRelationship
import snd.komf.mangabaka.model.MangaBakaSeriesTag
import snd.komf.mangabaka.model.MangaBakaTitle

private val json = Json {
    ignoreUnknownKeys = true
}

object MangaBakaSeriesImportTable : Table("series") {
    val id = long("id")
    val type = text("type")
    val canonicalUrl = text("canonical_url")
    val coverRawUrl = text("cover_raw_url").nullable()
    val coverRawSize = long("cover_raw_size").nullable()
    val coverRawWidth = integer("cover_raw_width").nullable()
    val coverRawFormat = text("cover_raw_format").nullable()
    val coverRawHeight = integer("cover_raw_height").nullable()
    val coverRawBlurhash = text("cover_raw_blurhash").nullable()
    val coverRawThumbhash = text("cover_raw_thumbhash").nullable()
    val coverX150X1 = text("cover_x150_x1").nullable()
    val coverX150X2 = text("cover_x150_x2").nullable()
    val coverX150X3 = text("cover_x150_x3").nullable()
    val coverX250X1 = text("cover_x250_x1").nullable()
    val coverX250X2 = text("cover_x250_x2").nullable()
    val coverX250X3 = text("cover_x250_x3").nullable()
    val coverX350X1 = text("cover_x350_x1").nullable()
    val coverX350X2 = text("cover_x350_x2").nullable()
    val coverX350X3 = text("cover_x350_x3").nullable()
    val state = text("state")
    val rating = double("rating").nullable()
    val status = text("status")
    val titles = json<List<MangaBakaTitle>>("titles", json).nullable()
    val artists = json<List<String>>("artists", json).nullable()
    val authors = json<List<String>>("authors", json).nullable()
    val tagsV2 = json<List<MangaBakaSeriesTag>>("tags_v2", json).nullable()
    val linksV2 = json<List<MangaBakaLink>>("links_v2", json).nullable()
    val hasAnime = bool("has_anime")
    val anime_end = text("anime_end").nullable()
    val anime_start = text("anime_start").nullable()
    val publishedEndDate = text("published_end_date").nullable()
    val publishedEndDateIsEstimated = bool("published_end_date_is_estimated").nullable()
    val publishedStartDate = text("published_start_date").nullable()
    val publishedStartDateIsEstimated = bool("published_start_date_is_estimated").nullable()
    val publishers = json<List<MangaBakaPublisher>>("publishers", json).nullable()
    val description = text("description").nullable()
    val isLicenced = bool("is_licensed")
    val mergedWith = long("merged_with").nullable()
    val finalVolume = text("final_volume").nullable()
    val contentRating = text("content_rating")
    val totalChapters = text("total_chapters").nullable()
    val lastUpdatedAt = text("last_updated_at")
    val relationshipsV2 = json<List<MangaBakaRelationship>>("relationships_v2", json).nullable()
    val originalLanguage = text("original_language").nullable()
    val sourceKitsuId = integer("source_kitsu_id").nullable()
    val sourceKitsuRating = double("source_kitsu_rating").nullable()
    val sourceKitsuRatingNormalized = integer("source_kitsu_rating_normalized").nullable()
    val sourceAniListId = integer("source_anilist_id").nullable()
    val sourceAniListRating = double("source_anilist_rating").nullable()
    val sourceAniListRatingNormalized = integer("source_anilist_rating_normalized").nullable()
    val sourceShikimoriId = integer("source_shikimori_id").nullable()
    val sourceShikimoriRating = double("source_shikimori_rating").nullable()
    val sourceShikimoriRatingNormalized = integer("source_shikimori_rating_normalized").nullable()
    val sourceAnimePlanetId = text("source_anime_planet_id").nullable()
    val sourceAnimePlanetRating = double("source_anime_planet_rating").nullable()
    val sourceAnimePlanetRatingNormalized = integer("source_anime_planet_rating_normalized").nullable()
    val sourceMangaUpdatesId = text("source_manga_updates_id").nullable()
    val sourceMangaUpdatesRating = double("source_manga_updates_rating").nullable()
    val sourceMangaUpdatesRatingNormalized = integer("source_manga_updates_rating_normalized").nullable()
    val sourceMyAnimeListId = integer("source_my_anime_list_id").nullable()
    val sourceMyAnimeListRating = double("source_my_anime_list_rating").nullable()
    val sourceMyAnimeListRatingNormalized = integer("source_my_anime_list_rating_normalized").nullable()
    val sourceAnimeNewsNetworkId = integer("source_anime_news_network_id").nullable()
    val sourceAnimeNewsNetworkRating = double("source_anime_news_network_rating").nullable()
    val sourceAnimeNewsNetworkRatingNormalized = integer("source_anime_news_network_rating_normalized").nullable()
}

