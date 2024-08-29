package snd.komf.mediaserver

import snd.komf.mediaserver.metadata.MetadataService
import snd.komf.mediaserver.metadata.MetadataUpdater

class MetadataServiceProvider(
    private val defaultMetadataService: MetadataService,
    private val libraryMetadataServices: Map<String, MetadataService>,

    private val defaultUpdateService: MetadataUpdater,
    private val libraryUpdaterServices: Map<String, MetadataUpdater>
) {
    fun defaultMetadataService() = defaultMetadataService
    fun defaultUpdateService() = defaultUpdateService

    fun metadataServiceFor(libraryId: String) = libraryMetadataServices[libraryId] ?: defaultMetadataService
    fun updateServiceFor(libraryId: String) = libraryUpdaterServices[libraryId] ?: defaultUpdateService
}
