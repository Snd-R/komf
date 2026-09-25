package snd.komf.mediaserver.match.repository.tables

import org.jetbrains.exposed.v1.core.Table

object SeriesThumbnailTable : Table("SeriesThumbnail") {
    val seriesId = text("seriesId")
    val thumbnailId = text("thumbnailId")
    val mediaServer = text("mediaServer")
}