package snd.komf.mediaserver.kavita

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


class KavitaTokenProvider(
    private val kavitaClient: KavitaAuthClient,
    private val apiKey: String,
    private val jwtConsumer: JwtConsumer,
    private val clock: Clock,
) {
    private var kavitaToken: KavitaAccessToken? = null
    private val tokenMutex = Mutex()

    suspend fun getToken(): String {
        val currentToken = kavitaToken
        return if (currentToken == null || isExpired(currentToken)) updateAndGetToken().value
        else currentToken.value
    }

    private suspend fun updateAndGetToken(): KavitaAccessToken {
        tokenMutex.withLock {
            val lockedToken = kavitaToken

            return if (lockedToken == null || isExpired(lockedToken)) {
                getFreshToken().also { kavitaToken = it }
            } else lockedToken
        }
    }

    private suspend fun getFreshToken(): KavitaAccessToken {
        val jwt = kavitaClient.authenticate(apiKey).token
        val expirationDate = jwtConsumer.processToExpirationDateClaim(jwt)
        return KavitaAccessToken(jwt, expirationDate)
    }

    private fun isExpired(token: KavitaAccessToken): Boolean {
        return clock.now() > token.expiresAt.minus(12.hours)
    }
}

data class KavitaAccessToken(
    val value: String,
    val expiresAt: Instant,
)
