package org.snd.metadata.providers.comicvine.model

import org.snd.metadata.model.metadata.ProviderBookId

@JvmInline
value class ComicVineIssueId(val id: Int)

fun ProviderBookId.toComicVineIssueId() = ComicVineIssueId(id.toInt())
