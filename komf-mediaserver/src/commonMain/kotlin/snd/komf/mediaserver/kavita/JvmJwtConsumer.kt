package snd.komf.mediaserver.kavita

import kotlinx.datetime.Instant
import org.jose4j.jwt.consumer.JwtConsumerBuilder

class JvmJwtConsumer : JwtConsumer {
    private val jwtConsumer = JwtConsumerBuilder().apply {
        setSkipSignatureVerification()
        setAllowedClockSkewInSeconds(600) // 10 minutes
    }.build()

    override fun processToExpirationDateClaim(jwt: String): Instant {
        val claims = jwtConsumer.processToClaims(jwt)
        return Instant.Companion.fromEpochMilliseconds(claims.expirationTime.valueInMillis)
    }
}