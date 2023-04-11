package org.snd.config

import kotlinx.serialization.Serializable
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.MediaType.MANGA
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.NameMatchingMode.CLOSEST_MATCH
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.AuthorRole.COLORIST
import org.snd.metadata.model.metadata.AuthorRole.COVER
import org.snd.metadata.model.metadata.AuthorRole.INKER
import org.snd.metadata.model.metadata.AuthorRole.LETTERER
import org.snd.metadata.model.metadata.AuthorRole.PENCILLER
import org.snd.metadata.model.metadata.AuthorRole.WRITER

@Serializable
data class MetadataProvidersConfig(
    val malClientId: String = "",
    val nameMatchingMode: NameMatchingMode = CLOSEST_MATCH,
    val defaultProviders: ProvidersConfig = ProvidersConfig(),
    val libraryProviders: Map<String, ProvidersConfig> = emptyMap(),

    @Deprecated("moved to default providers config")
    val mangaUpdates: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val mal: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val nautiljon: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val aniList: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val yenPress: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val kodansha: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val viz: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val bookWalker: ProviderConfig? = null,
)

@Serializable
data class ProvidersConfig(
    val mangaUpdates: ProviderConfig = ProviderConfig(),
    val mal: ProviderConfig = ProviderConfig(),
    val nautiljon: ProviderConfig = ProviderConfig(),
    val aniList: AniListConfig = AniListConfig(),
    val yenPress: ProviderConfig = ProviderConfig(),
    val kodansha: ProviderConfig = ProviderConfig(),
    val viz: ProviderConfig = ProviderConfig(),
    val bookWalker: ProviderConfig = ProviderConfig(),
    val mangaDex: ProviderConfig = ProviderConfig(),
    val bangumi: ProviderConfig = ProviderConfig(),
)

@Serializable
data class ProviderConfig(
    @Deprecated("moved to separate config")
    val clientId: String = "",
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val bookMetadata: BookMetadataConfig = BookMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
    val mediaType: MediaType = MANGA,

    val authorRoles: Collection<AuthorRole> = listOf(WRITER),
    val artistRoles: Collection<AuthorRole> = listOf(PENCILLER, INKER, COLORIST, LETTERER, COVER),
)

@Serializable
data class AniListConfig(
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
    val mediaType: MediaType = MANGA,

    val authorRoles: Collection<AuthorRole> = listOf(WRITER),
    val artistRoles: Collection<AuthorRole> = listOf(PENCILLER, INKER, COLORIST, LETTERER, COVER),
    val tagsScoreThreshold: Int = 60,
    val tagsSizeLimit: Int = 15,
)

@Serializable
data class SeriesMetadataConfig(
    val status: Boolean = true,
    val title: Boolean = true,
    val titleSort: Boolean = true,
    val summary: Boolean = true,
    val publisher: Boolean = true,
    val readingDirection: Boolean = true,
    val ageRating: Boolean = true,
    val language: Boolean = true,
    val genres: Boolean = true,
    val tags: Boolean = true,
    val totalBookCount: Boolean = true,
    val authors: Boolean = true,
    val releaseDate: Boolean = true,
    val thumbnail: Boolean = true,
    val books: Boolean = true,
    val links: Boolean = true,
    val score: Boolean = true,

    val useOriginalPublisher: Boolean = false,
    val originalPublisherTagName: String? = null,
    val englishPublisherTagName: String? = null,
    val frenchPublisherTagName: String? = null,
)

@Serializable
data class BookMetadataConfig(
    val title: Boolean = true,
    val summary: Boolean = true,
    val number: Boolean = true,
    val numberSort: Boolean = true,
    val releaseDate: Boolean = true,
    val authors: Boolean = true,
    val tags: Boolean = true,
    val isbn: Boolean = true,
    val links: Boolean = true,
    val thumbnail: Boolean = true,
)
