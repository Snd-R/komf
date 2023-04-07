package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.Json

/**
 * An enumeration.
 *
 * Values: producer,mangaka,artist,seiyu,writer,illustrator,actor
 */

enum class PersonCareer(val value: String) {
    @Json(name = "producer")
    PRODUCER("producer"),

    @Json(name = "mangaka")
    MANGAKA("mangaka"),

    @Json(name = "artist")
    ARTIST("artist"),

    @Json(name = "seiyu")
    SEIYU("seiyu"),

    @Json(name = "writer")
    WRITER("writer"),

    @Json(name = "illustrator")
    ILLUSTRATOR("illustrator"),

    @Json(name = "actor")
    ACTOR("actor");
}

