package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json
import snd.komf.mangabaka.model.MangaBakaTitleTrait

object TitlesTable : Table("titles") {
    val seriesId = long("id").references(SeriesTable.id).index()
    val title = text("title")
    val language = text("language")
    val traits = json<List<MangaBakaTitleTrait>>("traits", mangaBakaJson)
    val isPrimary = bool("is_primary")
    val note = text("note").nullable()
}