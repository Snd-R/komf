package snd.komf.mediaserver.kavita

import kotlinx.datetime.Instant

interface JwtConsumer {
    fun processToExpirationDateClaim(jwt: String): Instant
}