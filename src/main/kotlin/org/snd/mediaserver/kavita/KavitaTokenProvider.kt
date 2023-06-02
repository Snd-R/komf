package org.snd.mediaserver.kavita

import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.snd.mediaserver.kavita.model.KavitaAccessToken
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import java.util.*
import java.util.concurrent.locks.ReentrantLock
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

    @Volatile
    private var kavitaToken: KavitaAccessToken? = null
    private val tokenLock = ReentrantLock()

    fun getToken(): String {
        val token = kavitaToken
        return if (token == null || isExpired(token)) updateAndGetToken().token
        else token.token
    }

    private fun updateAndGetToken(): KavitaAccessToken {
        try {
            tokenLock.lock()
            val lockedToken = kavitaToken

            return if (lockedToken == null || isExpired(lockedToken))
                getFreshToken().also { kavitaToken = it }
            else lockedToken
        } finally {
            tokenLock.unlock()
        }
    }

    private fun getFreshToken(): KavitaAccessToken {
        val jwt = kavitaClient.authenticate(apiKey).token
        val claims = jwtConsumer.processToClaims(jwt)
        val expirationDate = Instant.ofEpochMilli(claims.expirationTime.valueInMillis)
        return KavitaAccessToken(jwt, expirationDate)
    }

    private fun isExpired(token: KavitaAccessToken): Boolean {
        return clock.instant().isAfter(token.expiresAt.minus(12, HOURS))
    }
}