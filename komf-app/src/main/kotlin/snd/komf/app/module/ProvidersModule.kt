package snd.komf.app.module

import io.ktor.client.*
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProviderFactory

class ProvidersModule(
    providersConfig: MetadataProvidersConfig,
    ktorBaseClient: HttpClient
) {
    private val providerFactory = ProviderFactory(ktorBaseClient)

    val metadataProviders = providerFactory.getMetadataProviders(providersConfig)
}