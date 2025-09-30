package snd.komf.mediaserver.kavita

import org.jose4j.jwt.consumer.JwtConsumerBuilder
import kotlin.time.Instant

class JvmJwtConsumer : JwtConsumer {
    private val jwtConsumer = JwtConsumerBuilder().apply {
        setSkipSignatureVerification()
        setAllowedClockSkewInSeconds(600) // 10 minutes
    }.build()

    override fun processToExpirationDateClaim(jwt: String): Instant {
        val claims = jwtConsumer.processToClaims(jwt)
        return Instant.fromEpochMilliseconds(claims.expirationTime.valueInMillis)
    }
}