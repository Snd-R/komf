package org.snd.metadata.providers.bangumi

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.TitleType.NATIVE
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.bangumi.model.Subject

class BangumiMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(subject: Subject, thumbnail: Image? = null): ProviderSeriesMetadata {
        // TODO: Does not have a field for end date. Have to get this from infobox
        val status = SeriesStatus.ENDED

        // TODO: use persons to get list of persons and find author
        val author = subject.infobox?.find { it.key == "作者" }
        val authors = listOf(Author(author?.value as String, AuthorRole.WRITER))

        val tags = subject.tags.map{it.name}

        val titles = listOf(
            SeriesTitle(subject.name_cn, NATIVE, "zh"),
            SeriesTitle(subject.name, LOCALIZED, "ja"),
        )

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = subject.summary,
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            links = listOf(WebLink("Bangumi", "")),
            score = subject.rating.score.toDouble()
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(subject.id.toString()), metadata = metadata),
            metadataConfig
        )
    }
}
