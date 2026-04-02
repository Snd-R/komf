package snd.komf.providers

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ProvidersModuleTest {

    @Test
    fun `strip info is exposed when enabled in default providers`() = runBlocking {
        val httpClient = HttpClient(OkHttp) {
            install(UserAgent) { agent = "Komf/test" }
        }

        try {
            val config = MetadataProvidersConfig(
                defaultProviders = ProvidersConfig(
                    stripInfo = ProviderConfig(
                        enabled = true,
                        priority = 1
                    )
                )
            )

            val providers = ProvidersModule(
                config = config,
                baseHttpClient = httpClient,
                mangaBakaDatabase = null
            ).getMetadataProviders()
                .defaultProvidersList()
                .map { it.providerName() }

            assertEquals(listOf(CoreProviders.STRIP_INFO), providers)
        } finally {
            httpClient.close()
        }
    }
}
