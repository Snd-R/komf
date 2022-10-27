package org.snd.mediaserver.kavita.model

import java.time.Instant

data class KavitaAccessToken(
    val token: String,
    val expiresAt: Instant,
)