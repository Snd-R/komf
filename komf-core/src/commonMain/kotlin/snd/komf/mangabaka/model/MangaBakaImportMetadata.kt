package snd.komf.mangabaka.model

import kotlin.time.Instant

data class MangaBakaImportMetadata(
    val timestamp: Instant,
    val checksum: String,
)