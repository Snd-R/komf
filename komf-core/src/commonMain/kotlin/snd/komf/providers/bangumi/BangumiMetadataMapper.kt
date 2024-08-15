package snd.komf.providers.bangumi

import kotlinx.datetime.LocalDate
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.bangumi.model.BangumiSubject
import snd.komf.providers.bangumi.model.Infobox
import snd.komf.providers.bangumi.model.SubjectRelation
import snd.komf.providers.bangumi.model.SubjectSearchData

class BangumiMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val subjectBaseUrl = "https://bgm.tv/subject/"
    private val bookNumberRegex = "\\(([^)]*)\\)[^(]*\$".toRegex()

    fun toSeriesMetadata(
        subject: BangumiSubject,
        bookRelations: Collection<SubjectRelation>,
        thumbnail: Image?,
    ): ProviderSeriesMetadata {
        val infoBox = subject.infobox?.associate { it.key to it } ?: emptyMap()
        val endDateRaw = when (val endDate = infoBox["结束"]) {
            is Infobox.SingleValue -> endDate.value
            else -> null
        }
        // Does not seem to have Abandon info
        val status = if (endDateRaw == null) {
            SeriesStatus.ONGOING
        } else if (endDateRaw.contains("休刊")) {
            SeriesStatus.HIATUS
        } else {
            SeriesStatus.ENDED
        }

        val tags = subject.tags.sortedByDescending { it.count }
            .take(15)
            .filter { it.count > 1 }
            .map { it.name }

        val altTitles =
            when (val infoBoxAliases = infoBox["别名"]) {
                is Infobox.SingleValue -> listOf(null to infoBoxAliases.value)
                is Infobox.MultipleValues -> infoBoxAliases.value.map { it.key to it.value }
                null -> emptyList()
            }
                .map { (language, name) -> if (language == "hk") name to "zh-hk" else name to language }
                .map { (title, language) -> SeriesTitle(title, null, language) }

        val titles = listOfNotNull(
            SeriesTitle(subject.name, null, null),
            subject.nameCn.ifBlank { null }?.let { SeriesTitle(subject.nameCn, null, "zh") },
        ) + altTitles

        val publishers = when (val publisherInfo = infoBox["出版社"]) {
            is Infobox.SingleValue -> publisherInfo.value.split(',', '，', '、')
            else -> emptyList()
        }
        val otherPublishers = when (val publisherInfo = infoBox["其他出版社"]) {
            is Infobox.SingleValue -> publisherInfo.value.split(',', '，', '、')
            else -> emptyList()
        }

        val altPublishers = publishers.drop(1).plus(otherPublishers)

        val releaseDate = subject.date?.ifBlank { null }?.let { LocalDate.parse(it) }
        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = subject.summary,
            publisher = publishers.firstOrNull(),
            alternativePublishers = altPublishers.toSet(),
            tags = tags,
            authors = getAuthors(infoBox),
            releaseDate = ReleaseDate(
                releaseDate?.year,
                releaseDate?.monthNumber,
                releaseDate?.dayOfMonth
            ),
            links = listOf(WebLink("Bangumi", subjectUrl(subject.id))),
            score = subject.rating.score,
            thumbnail = thumbnail,
        )

        val books = bookRelations.map {
            SeriesBook(
                id = ProviderBookId(it.id.toString()),
                number = getBookNumber(it.name),
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
        book: BangumiSubject,
        thumbnail: Image?
    ): ProviderBookMetadata {
        val tags = book.tags.asSequence()
            .sortedByDescending { it.count }
            .take(15)
            .filter { it.count > 1 }
            .map { it.name }.toSet()

        val infoBox = book.infobox?.associate { it.key to it } ?: emptyMap()
        val bookNumber = getBookNumber(book.name)

        val isbn = when (val isbn = infoBox["ISBN-13"]) {
            is Infobox.SingleValue -> isbn.value
            else -> null
        } ?: when (val isbn = infoBox["ISBN"]) {
            is Infobox.SingleValue -> isbn10ToIsbn13(isbn.value)
            else -> null
        }
        val metadata = BookMetadata(
            title = book.name,
            summary = book.summary,
            number = bookNumber,
            numberSort = bookNumber?.start,
            releaseDate = book.date?.let { LocalDate.parse(it) },
            authors = getAuthors(infoBox),
            tags = tags,
            isbn = isbn,
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(WebLink("Bangumi", subjectUrl(book.id))),
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.toString()),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun getBookNumber(name: String): BookRange? {
        return bookNumberRegex.find(name)?.groups?.last()?.value?.toDoubleOrNull()
            ?.let { number -> BookRange(number, number) }
    }

    private fun getAuthors(infoBox: Map<String, Infobox>): List<Author> {
        val author = when (val author = infoBox["作者"]) {
            is Infobox.SingleValue -> author.value
            else -> null
        }

        val originalCreator = when (val originalCreator = infoBox["原作"]) {
            is Infobox.SingleValue -> originalCreator.value
            else -> null
        }
        val illustrator = when (val illustrator = infoBox["作画"]) {
            is Infobox.SingleValue -> illustrator.value
            else -> null
        }
        val characterDesign = when (val characterDesign = infoBox["人物原案"]) {
            is Infobox.SingleValue -> characterDesign.value
            else -> null
        }


        val authors = mutableListOf<Author>()
        author?.let { authorName ->
            (authorRoles + artistRoles).forEach { authors.add(Author(name = authorName, role = it)) }
        }
        originalCreator?.let { authorName ->
            authorRoles.forEach { authors.add(Author(name = authorName, role = it)) }
        }

        illustrator?.let { authorName ->
            artistRoles.forEach { authors.add(Author(name = authorName, role = it)) }
        }
        characterDesign?.let { authorName ->
            artistRoles.forEach { authors.add(Author(name = authorName, role = it)) }
        }
        return authors
    }

    private fun isbn10ToIsbn13(isbn10: String): String {
        val isbnIntermediate = "978" + isbn10.substring(0, 9)

        var sum = 0
        for (index in isbnIntermediate.indices) {
            val d = if (index % 2 == 0) 1 else 3
            sum += isbnIntermediate[index].digitToInt() * d
        }

        val mod = sum % 10
        val checkDigit = if (mod == 0) 0 else 10 - mod
        return isbnIntermediate + checkDigit
    }

    fun toSearchResult(searchData: SubjectSearchData): SeriesSearchResult {
        return SeriesSearchResult(
            url = subjectUrl(searchData.id),
            imageUrl = searchData.image,
            provider = CoreProviders.BANGUMI,
            resultId = searchData.id.toString(),
            title = searchData.nameCn.ifBlank { searchData.name },
        )
    }

    private fun subjectUrl(id: Long) = subjectBaseUrl + id
}
