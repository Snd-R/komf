package org.snd.metadata.providers.comicvine.model

import org.snd.metadata.model.metadata.ProviderSeriesId

@JvmInline
value class ComicVineVolumeId(val id: Int)

fun ProviderSeriesId.toComicVineVolumeId() = ComicVineVolumeId(id.toInt())
