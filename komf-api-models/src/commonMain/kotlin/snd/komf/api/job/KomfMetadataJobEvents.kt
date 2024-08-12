package snd.komf.api.job

import kotlinx.serialization.Serializable
import snd.komf.api.KomfProviders

const val providerSeriesEventName = "ProviderSeriesEvent"
const val providerBookEventName = "ProviderBookEvent"
const val providerCompletedEventName = "ProviderCompletedEvent"
const val providerErrorEventName = "ProviderErrorEvent"
const val postProcessingStartName = "PostProcessingStartEvent"
const val processingErrorEvent = "ProcessingErrorEvent"
const val eventsStreamNotFoundName = "EventStreamNotFoundEvent"

@Serializable
sealed interface KomfMetadataJobEvent {

    @Serializable
    data class ProviderSeriesEvent(
        val provider: KomfProviders,
    ) : KomfMetadataJobEvent

    @Serializable
    data class ProviderBookEvent(
        val provider: KomfProviders,
        val totalBooks: Int,
        val bookProgress: Int,
    ) : KomfMetadataJobEvent


    @Serializable
    data class ProviderErrorEvent(
        val provider: KomfProviders,
        val message: String
    ) : KomfMetadataJobEvent

    @Serializable
    data class ProviderCompletedEvent(
        val provider: KomfProviders,
    ) : KomfMetadataJobEvent

    @Serializable
    data object PostProcessingStartEvent : KomfMetadataJobEvent

    @Serializable
    data class ProcessingErrorEvent(val message: String) : KomfMetadataJobEvent

    @Serializable
    data object NotFound : KomfMetadataJobEvent

    @Serializable
    data object UnknownEvent : KomfMetadataJobEvent
}
