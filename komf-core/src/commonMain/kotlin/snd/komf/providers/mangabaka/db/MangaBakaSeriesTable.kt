package snd.komf.providers.mangabaka.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

private val json = Json.Default

object MangaBakaSeriesTable : Table("series") {
    val id = integer("id")
    val state = text("state")
    val mergedWith = integer("merged_with").nullable()

    val title = text("title")
    val nativeTitle = text("native_title").nullable()
    val romanizedTitle = text("romanized_title").nullable()
    val secondaryTitlesEn = json<List<MangaBakaDbSecondaryTitle>>("secondary_titles_en", json).nullable()
    val coverRawUrl = text("cover_raw_url").nullable()
    val coverRawSize = text("cover_raw_size").nullable()
    val coverRawWidth = text("cover_raw_width").nullable()
    val coverRawHeight = text("cover_raw_height").nullable()
    val coverRawFormat = text("cover_raw_format").nullable()
    val coverRawBlurhash = text("cover_raw_blurhash").nullable()
    val coverRawThumbhash = text("cover_raw_thumbhash").nullable()
    val coverX350X1Url = text("cover_x350_x1").nullable()

    val authors = json<List<String>>("authors", json).nullable()
    val artists = json<List<String>>("artists", json).nullable()
    val description = text("description").nullable()
    val year = integer("year").nullable()
    val status = text("status")
    val isLicenced = bool("is_licensed")
    val hasAnime = bool("has_anime")
    val contentRating = text("content_rating")
    val type = text("type")
    val rating = double("rating").nullable()
    val finalVolume = text("final_volume").nullable()
    val finalChapter = text("final_chapter").nullable()
    val totalChapters = text("total_chapters").nullable()
    val links = json<List<String>>("links", json)
    val publishers = json<List<MangaBakaDbPublisher>>("publishers", json).nullable()
    val genres = json<List<String>>("genres", json).nullable()
    val tags = json<List<String>>("tags", json).nullable()

    val sourceAnilistId = integer("source_anilist_id").nullable()
    val sourceAnilistRating = double("source_anilist_rating").nullable()
    val sourceAnimeNewNetworkId = integer("source_anime_news_network_id").nullable()
    val sourceAnimeNewNetworkRating = double("source_anime_news_network_rating").nullable()
    val sourceMangaDexId = text("source_mangadex_id").nullable()
    val sourceMangaDexRating = double("source_mangadex_rating").nullable()
    val sourceMangaUpdatesId = text("source_manga_updates_id").nullable()
    val sourceMangaUpdatesRating = double("source_manga_updates_rating").nullable()
    val sourceMyAnimeListId = integer("source_my_anime_list_id").nullable()
    val sourceMyAnimeListRating = double("source_my_anime_list_rating").nullable()
    val sourceKitsuId = integer("source_kitsu_id").nullable()
    val sourceKitsuRating = double("source_kitsu_rating").nullable()

    val relationshipsOther = json<List<Int>>("relationships_other", json).nullable()
    val relationshipsSequel = json<List<Int>>("relationships_sequel", json).nullable()
    val relationshipsMainStory = json<List<Int>>("relationships_main_story", json).nullable()
    val relationshipsSideStory = json<List<Int>>("relationships_side_story", json).nullable()
    val relationshipsAlternative = json<List<Int>>("relationships_alternative", json).nullable()
    val relationshipsPrequel = json<List<Int>>("relationships_prequel", json).nullable()
    val relationshipsAdaptation = json<List<Int>>("relationships_adaptation", json).nullable()
    val relationshipsSpinOff = json<List<Int>>("relationships_spin_off", json).nullable()

    val lastUpdatedAt = text("last_updated_at")

    @Serializable
    data class MangaBakaDbSecondaryTitle(
        val type: String,
        val title: String,
        val note: String?,
    )

    @Serializable
    data class MangaBakaDbPublisher(
        val type: String,
        val name: String,
        val note: String?,
    )
}

