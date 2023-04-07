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
import org.snd.metadata.providers.bangumi.model.PersonCareer
import org.snd.metadata.providers.bangumi.model.RelatedPerson
import org.snd.metadata.providers.bangumi.model.Subject
import org.snd.metadata.providers.mangaupdates.model.Publisher
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
        thumbnail: Image? = null,
        relatedPersons: Collection<RelatedPerson>? = null,
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

        val publisher =
            subject.infobox?.find { it.key == "出版社" }?.value?.rawString

        val originalAuthor = subject.infobox?.filter {
            it.key in listOf("原作", "作者")
        }?.maxByOrNull { it.key == "原作" }?.let { listOf(Author(it.value.rawString, AuthorRole.WRITER)) }
            ?: listOf()

        val additionalAuthors = relatedPersons?.mapNotNull { person ->
            when (person.career.first()) {
                PersonCareer.MANGAKA -> Author(person.name, AuthorRole.WRITER)
                else -> null
            }
        } ?: listOf()

        val authors = (originalAuthor + additionalAuthors).flatMap {
            when (it.role) {
                AuthorRole.WRITER -> authorRoles.map { role -> Author(it.name, role) }
                else -> artistRoles.map { role -> Author(it.name, role) }
            }
        }.distinct()

        val alternativePublishers = relatedPersons?.mapNotNull { person ->
            when (person.career.first()) {
                PersonCareer.PRODUCER -> Publisher(
                    person.id.toLong(), person.name, person.relation, person.type.toString()
                )

                else -> null
            }
        } ?: listOf()

        val tags = subject.tags.sortedByDescending { it.count }.take(15)
            .map { it.name }

        val altTitles =
            subject.infobox?.find { it.key == "别名" }?.value?.list?.flatMap { it.values }
                ?.map { title -> SeriesTitle(title, null, null) }
                ?: listOf()

        val titles = listOf(
            SeriesTitle(subject.nameCn, NATIVE, "zh"),
            SeriesTitle(subject.name, LOCALIZED, "ja"),
        ) + altTitles

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val releaseDate = if (!subject.date.isNullOrBlank()) LocalDate.parse(subject.date, formatter) else null

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = subject.summary,
            tags = tags,
            authors = authors,
            publisher = publisher,
            alternativePublishers = alternativePublishers.map { it.name }.toSet(),
            thumbnail = thumbnail,
            totalBookCount = subject.volumes,
            releaseDate = ReleaseDate(
                releaseDate?.year,
                releaseDate?.monthValue,
                releaseDate?.dayOfMonth
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
