package snd.komf.providers.mangabaka.db

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json
import snd.komf.providers.mangabaka.MangaBakaLink
import snd.komf.providers.mangabaka.MangaBakaPublisher
import snd.komf.providers.mangabaka.MangaBakaRelationship
import snd.komf.providers.mangabaka.MangaBakaTags
import snd.komf.providers.mangabaka.MangaBakaTitle

private val json = Json {
    ignoreUnknownKeys = true
}

object MangaBakaSeriesTable : Table("series") {
    val id = integer("id")
    val type = text("type")
    val coverRawUrl = text("cover_raw_url").nullable()
    val coverRawSize = integer("cover_raw_size").nullable()
    val coverRawWidth = integer("cover_raw_width").nullable()
    val coverRawFormat = text("cover_raw_format").nullable()
    val coverRawHeight = integer("cover_raw_height").nullable()
    val coverRawBlurhash = text("cover_raw_blurhash").nullable()
    val coverRawThumbhash = text("cover_raw_thumbhash").nullable()
    val coverX350X1Url = text("cover_x350_x1").nullable()
    val state = text("state")
    val rating = double("rating").nullable()
    val status = text("status")
    val titles = json<List<MangaBakaTitle>>("titles", json).nullable()
    val artists = json<List<String>>("artists", json).nullable()
    val authors = json<List<String>>("authors", json).nullable()
    val tagsV2 = json<List<MangaBakaTags>>("tags_v2", json).nullable()
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
    val mergedWith = integer("merged_with").nullable()
    val finalVolume = text("final_volume").nullable()
    val contentRating = text("content_rating")
    val totalChapters = text("total_chapters").nullable()
    val lastUpdatedAt = text("last_updated_at")
    val relationshipsV2 = json<List<MangaBakaRelationship>>("relationships_v2", json).nullable()
}

