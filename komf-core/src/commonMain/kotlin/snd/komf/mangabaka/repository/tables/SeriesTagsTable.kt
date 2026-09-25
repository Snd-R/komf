package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json
import snd.komf.mangabaka.model.MangaBakaTagId

object SeriesTagsTable : Table("series_tags") {
    val seriesId = long("series_id").references(SeriesTable.id).index()
    val tagId = long("tag_id").references(TagsTable.id).index()
    val isSpoiler = bool("is_spoiler")
    val isExplicit = bool("is_explicit")
    val impliedByTagIds = json<List<MangaBakaTagId>>("implied_by_tag_ids", mangaBakaJson)
    val weight = text("weight")

    override val primaryKey = PrimaryKey(tagId, seriesId)
}