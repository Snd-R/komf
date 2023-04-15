package org.snd.metadata.providers.bangumi

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.BookRange
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.bangumi.model.Subject
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.AUTHOR
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.CHARACTER_DESIGN
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.ILLUSTRATOR
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.ORIGINAL_CREATOR
import org.snd.metadata.providers.bangumi.model.SubjectRelation

class BangumiMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val subjectBaseUrl = "https://bgm.tv/subject/"
    private val bookNumberRegex = "\\(([^)]*)\\)[^(]*\$".toRegex()

    fun toSeriesMetadata(
        subject: Subject,
        bookRelations: Collection<SubjectRelation>,
        thumbnail: Image?,
    ): ProviderSeriesMetadata {
        // Does not seem to have Abandon info
        val status = if (subject.endDateRaw == null) {
            SeriesStatus.ONGOING
        } else if (subject.endDateRaw.contains("休刊")) {
            SeriesStatus.HIATUS
        } else {
            SeriesStatus.ENDED
        }

        val tags = subject.tags.sortedByDescending { it.count }
            .take(15)
            .filter { it.count > 1 }
            .map { it.name }

        val altTitles = subject.aliases
            .map { if (it.language == "hk") it.value to "zh-hk" else it.value to it.language }
            .map { (title, language) -> SeriesTitle(title, null, language) }

        val titles = listOfNotNull(
            SeriesTitle(subject.name, null, null),
            subject.nameCn?.let { SeriesTitle(subject.nameCn, null, "zh") },
        ) + altTitles

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = subject.summary,
            publisher = subject.publisher,
            alternativePublishers = subject.otherPublishers.toSet(),
            tags = tags,
            authors = getAuthors(subject),
            releaseDate = ReleaseDate(
                subject.date?.year,
                subject.date?.monthValue,
                subject.date?.dayOfMonth
            ),
            links = listOf(WebLink("Bangumi", subjectBaseUrl + subject.id)),
            score = subject.rating.score.toDouble(),
            thumbnail = thumbnail,
        )

        val books = bookRelations.map {
            SeriesBook(
                id = ProviderBookId(it.id.toString()),
                number = bookNumberRegex.find(it.name)?.groups?.last()?.value
                    ?.let { number -> BookRange(number.toDouble(), number.toDouble()) },
                name = it.name,
                type = null,
                edition = null
            )
        }

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId(subject.id.toString()),
                metadata = metadata,
                books = books
            ),
            seriesMetadataConfig
        )
    }

    fun toBookMetadata(
        book: Subject,
        thumbnail: Image?
    ): ProviderBookMetadata {
        val tags = book.tags.asSequence()
            .sortedByDescending { it.count }
            .take(15)
            .filter { it.count > 1 }
            .map { it.name }.toSet()

        val bookNumber = getBookNumber(book.name)
        val metadata = BookMetadata(
            title = book.name,
            summary = book.summary,
            number = bookNumber,
            numberSort = bookNumber?.start,
            releaseDate = book.date,
            authors = getAuthors(book),
            tags = tags,
            isbn = book.isbn,
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(WebLink("Bangumi", subjectBaseUrl + book.id)),
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.toString()),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun getBookNumber(name: String): BookRange? {
        return bookNumberRegex.find(name)?.groups?.last()?.value
            ?.let { number -> BookRange(number.toDouble(), number.toDouble()) }
    }

    private fun getAuthors(subject: Subject): List<Author> {
        return subject.authors.flatMap { subjectAuthor ->
            when (subjectAuthor.role) {
                AUTHOR -> (authorRoles + artistRoles).map { Author(name = subjectAuthor.name, role = it) }
                ORIGINAL_CREATOR -> authorRoles.map { Author(name = subjectAuthor.name, role = it) }
                ILLUSTRATOR, CHARACTER_DESIGN -> artistRoles.map { Author(name = subjectAuthor.name, role = it) }
            }
        }
    }
}
