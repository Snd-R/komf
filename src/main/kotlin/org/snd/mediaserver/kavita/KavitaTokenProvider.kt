package org.snd.mediaserver.kavita

import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.snd.mediaserver.kavita.model.KavitaAccessToken
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.*
import javax.crypto.spec.SecretKeySpec

class KavitaTokenProvider(
    private val kavitaClient: KavitaAuthClient,
    private val apiKey: String,
    private val verificationKey: String?,
    private val clock: Clock
) {
    private val jwtConsumer = JwtConsumerBuilder().apply {
        if (verificationKey != null)
            setVerificationKey(SecretKeySpec(Base64.getDecoder().decode(verificationKey), "HmacSHA512"))
        else setSkipSignatureVerification()
    }.build()

    private var kavitaToken: KavitaAccessToken? = null

    @Synchronized
    fun getToken(): String {
        val token = kavitaToken

        return if (token == null) {
            getFreshToken().also { kavitaToken = it }.token
        } else if (clock.instant().isAfter(token.expiresAt.minus(1, DAYS))) {
            getFreshToken().also { kavitaToken = it }.token
        } else token.token
    }

    private fun getFreshToken(): KavitaAccessToken {
        val jwt = kavitaClient.authenticate(apiKey).token
        val claims = jwtConsumer.processToClaims(jwt)
        val expirationDate = Instant.ofEpochMilli(claims.expirationTime.valueInMillis)
        return KavitaAccessToken(jwt, expirationDate)
    }
}