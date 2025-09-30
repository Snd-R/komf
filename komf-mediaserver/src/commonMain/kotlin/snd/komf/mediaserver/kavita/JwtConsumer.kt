package snd.komf.mediaserver.kavita

import kotlin.time.Instant

interface JwtConsumer {
    fun processToExpirationDateClaim(jwt: String): Instant
}