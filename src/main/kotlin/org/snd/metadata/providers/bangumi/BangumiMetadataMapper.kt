package org.snd.metadata.providers.bangumi

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.TitleType.NATIVE
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.bangumi.model.Subject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BangumiMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val subjectBaseUrl = "https://bgm.tv/subject/"

    fun toSeriesMetadata(
        subject: Subject,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val endStatus = subject.infobox?.find { it.key == "结束" }

        // Does not seem to have Abandon info
        val status = if (endStatus == null) {
            SeriesStatus.ONGOING
        } else if (endStatus.value.toString().contains("休刊")) {
            SeriesStatus.HIATUS
        } else {
            SeriesStatus.ENDED
        }

        // TODO: use API {subject_id}/persons to get full list of publishers
        val publisher =
            subject.infobox?.find { it.key == "出版社" }?.value?.rawString

        // TODO: use API {subject_id}/persons to get full list of persons
        val author =
            subject.infobox?.find { it.key == "作者" }?.value?.rawString?.let {
                Author( it, AuthorRole.WRITER )
            }
        val authors = listOfNotNull(author)

        val tags = subject.tags.sortedByDescending { it.count }.take(15)
            .map { it.name }

        // TODO: parse alternative titles from infobox
        val altTitles =
            subject.infobox?.find { it.key == "别名" }?.value?.list?.flatMap { it.values }
                ?.map { title -> SeriesTitle(title, null, null) }
                ?: listOf()

        val titles = listOf(
            SeriesTitle(subject.name_cn, NATIVE, "zh"),
            SeriesTitle(subject.name, LOCALIZED, "ja"),
        ) + altTitles

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val releaseDate = LocalDate.parse(subject.date, formatter)

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = subject.summary,
            tags = tags,
            authors = authors,
            publisher = publisher,
//            alternativePublishers = altTitles.toSet(),
            thumbnail = thumbnail,
            totalBookCount = subject.volumes,
            releaseDate = ReleaseDate(
                releaseDate.year,
                releaseDate.monthValue,
                releaseDate.dayOfMonth
            ),
            links = listOf(WebLink("Bangumi", subjectBaseUrl + subject.id)),
            score = subject.rating.score.toDouble()
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId(subject.id.toString()),
                metadata = metadata
            ),
            metadataConfig
        )
    }
}
