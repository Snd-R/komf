package snd.komf.mediaserver

import snd.komf.mediaserver.metadata.MetadataService
import snd.komf.mediaserver.metadata.MetadataUpdater

class MetadataServiceProvider(
    private val defaultMetadataService: MetadataService,
    private val libraryMetadataServices: Map<String, MetadataService>,

    private val defaultUpdateService: MetadataUpdater,
    private val libraryUpdaterServices: Map<String, MetadataUpdater>
) {
    fun defaultFetcherService() = defaultMetadataService
    fun defaultUpdateService() = defaultUpdateService

    fun fetcherServiceFor(libraryId: String) = libraryMetadataServices[libraryId] ?: defaultMetadataService
    fun updateServiceFor(libraryId: String) = libraryUpdaterServices[libraryId] ?: defaultUpdateService
}
