package org.snd.metadata.providers.mangadex.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArt
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArtId

class MangaDexCoverArtJsonAdapter {
    @FromJson
    fun fromJson(json: MangaDexCoverArtJson): MangaDexCoverArt {
        return MangaDexCoverArt(
            id = MangaDexCoverArtId(json.id),
            description = json.attributes.description,
            volume = json.attributes.volume,
            fileName = json.attributes.fileName,
            locale = json.attributes.locale,
            createdAt = json.attributes.createdAt,
            updatedAt = json.attributes.updatedAt,
            version = json.attributes.version
        )
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") cover: MangaDexCoverArt): MangaDexCoverArtJson {
        throw UnsupportedOperationException()
    }
}
