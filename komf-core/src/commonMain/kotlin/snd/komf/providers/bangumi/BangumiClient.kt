package snd.komf.providers.bangumi

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.providers.bangumi.model.BangumiSubject
import snd.komf.providers.bangumi.model.SearchSubjectsResponse
import snd.komf.providers.bangumi.model.SubjectRelation
import snd.komf.providers.bangumi.model.SubjectType

class BangumiClient(
    private val ktor: HttpClient,
) {
    private val apiV0Url = "https://api.bgm.tv/v0"

    suspend fun searchSeries(
        keyword: String,
        rating: Collection<String> = listOf(">0.0"), // Use min rating to improve result quality
        rank: Collection<String> = listOf(">=0"), // Use ranked items to improve result quality
    ): SearchSubjectsResponse {
        return ktor.post("$apiV0Url/search/subjects") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "keyword" to keyword,
                    "filter" to mapOf(
                        "type" to listOf(SubjectType.BOOK.value), // supports multiple, only use MANGA for now
                        "rating" to rating,
                        "rank" to rank,
                    )
                )
            )

        }.body()
    }

    suspend fun getSubject(subjectId: Long): BangumiSubject {
        return ktor.get("$apiV0Url/subjects/$subjectId").body()
    }

    suspend fun getSubjectRelations(subjectId: Long): Collection<SubjectRelation> {
        return ktor.get("$apiV0Url/subjects/$subjectId/subjects").body()
    }

    suspend fun getThumbnail(subject: BangumiSubject): Image? {
        return (subject.images.common ?: subject.images.medium)?.let {
            val bytes: ByteArray = ktor.get(it).body()
            Image(bytes)
        }
    }
}
