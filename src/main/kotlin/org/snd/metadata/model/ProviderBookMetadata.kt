package org.snd.metadata.model


data class ProviderBookMetadata(
    val id: ProviderBookId? = null,
    val provider: Provider,
    val metadata: BookMetadata,
)
