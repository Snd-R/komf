package snd.komf.mediaserver.match.repository.tables

import org.jetbrains.exposed.v1.core.Table

object BookThumbnailTable : Table("BookThumbnail") {
    val bookId = text("bookId")
    val seriesId = text("seriesId")
    val thumbnailId = text("thumbnailId")
    val mediaServer = text("mediaServer")
}