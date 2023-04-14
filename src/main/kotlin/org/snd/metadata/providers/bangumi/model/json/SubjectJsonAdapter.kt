package org.snd.metadata.providers.bangumi.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.apache.commons.validator.routines.ISBNValidator
import org.snd.metadata.providers.bangumi.model.Subject
import org.snd.metadata.providers.bangumi.model.SubjectAlias
import org.snd.metadata.providers.bangumi.model.SubjectAuthor
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.AUTHOR
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.CHARACTER_DESIGN
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.ILLUSTRATOR
import org.snd.metadata.providers.bangumi.model.SubjectAuthorRole.ORIGINAL_CREATOR
import java.time.LocalDate

class SubjectJsonAdapter {

    @FromJson
    fun fromJson(json: SubjectJson): Subject {
        val infoBox = json.infobox?.associate { it.key to it.value } ?: emptyMap()

        val publishers = when (val publisherInfo = infoBox["出版社"]) {
            is InfoboxValue.SingleValue -> publisherInfo.value.split(',', '，', '、')
            else -> emptyList()
        }
        val otherPublishers = when (val publisherInfo = infoBox["其他出版社"]) {
            is InfoboxValue.SingleValue -> publisherInfo.value.split(',', '，', '、')
            else -> emptyList()
        }

        val altPublishers = publishers.drop(1).plus(otherPublishers)

        val endDateRaw = when (val endDate = infoBox["结束"]) {
            is InfoboxValue.SingleValue -> endDate.value
            else -> null
        }
        val aliases = when (val aliases = infoBox["别名"]) {
            is InfoboxValue.SingleValue -> listOf(SubjectAlias(null, aliases.value))
            is InfoboxValue.MultipleValues -> aliases.value.map {
                when (it) {
                    is InfoboxNestedValue.SingleValue -> SubjectAlias(null, it.value)
                    is InfoboxNestedValue.PairValue -> SubjectAlias(it.key, it.value)
                }
            }

            null -> emptyList()
        }

        val isbn = when (val isbn = infoBox["ISBN"]) {
            is InfoboxValue.SingleValue -> ISBNValidator.getInstance().validate(isbn.value)
            else -> null
        }

        val author = when (val author = infoBox["作者"]) {
            is InfoboxValue.SingleValue -> author.value
            else -> null
        }

        val originalCreator = when (val originalCreator = infoBox["原作"]) {
            is InfoboxValue.SingleValue -> originalCreator.value
            else -> null
        }
        val illustrator = when (val illustrator = infoBox["作画"]) {
            is InfoboxValue.SingleValue -> illustrator.value
            else -> null
        }
        val characterDesign = when (val characterDesign = infoBox["人物原案"]) {
            is InfoboxValue.SingleValue -> characterDesign.value
            else -> null
        }
        val authors = listOfNotNull(
            author?.let { SubjectAuthor(AUTHOR, it) },
            illustrator?.let { SubjectAuthor(ILLUSTRATOR, it) },
            originalCreator?.let { SubjectAuthor(ORIGINAL_CREATOR, it) },
            characterDesign?.let { SubjectAuthor(CHARACTER_DESIGN, it) },
        )

        return Subject(
            id = json.id,
            type = json.type,
            name = json.name,
            nameCn = json.nameCn.ifBlank { null },
            summary = json.summary,
            nsfw = json.nsfw,
            locked = json.locked,
            platform = json.platform,
            images = json.images,
            rating = json.rating,
            collection = json.collection,
            tags = json.tags,
            date = json.date?.let { LocalDate.parse(it) },
            publisher = publishers.firstOrNull(),
            otherPublishers = altPublishers,
            endDateRaw = endDateRaw,
            aliases = aliases,
            isbn = isbn,
            authors = authors,
        )
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") subject: Subject): SubjectJson {
        throw UnsupportedOperationException()
    }
}