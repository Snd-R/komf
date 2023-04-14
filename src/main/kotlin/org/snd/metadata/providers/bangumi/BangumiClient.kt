package org.snd.metadata.providers.bangumi

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.common.http.HttpClient
import org.snd.common.http.MEDIA_TYPE_JSON
import org.snd.metadata.model.Image
import org.snd.metadata.providers.bangumi.model.SearchSubjectsResponse
import org.snd.metadata.providers.bangumi.model.Subject
import org.snd.metadata.providers.bangumi.model.SubjectRelation
import org.snd.metadata.providers.bangumi.model.SubjectType

class BangumiClient(
    private val client: HttpClient,
    private val moshi: Moshi
) {
    private val apiV0Url: HttpUrl = "https://api.bgm.tv/v0".toHttpUrl()

    fun searchSeries(
        keyword: String,
        rating: Collection<String> = listOf(">0.0"), // Use min rating to improve result quality
        rank: Collection<String> = listOf(">=0"), // Use ranked items to improve result quality
    ): SearchSubjectsResponse {
        val payload = moshi.adapter<Map<String, *>>().toJson(
            mapOf(
                "keyword" to keyword,
                "filter" to mapOf(
                    "type" to listOf(SubjectType.BOOK.value), // supports multiple, only use MANGA for now
                    "rating" to rating,
                    "rank" to rank,
                )
            )
        )
        val request = Request.Builder().url(
            apiV0Url.newBuilder().addPathSegments("search/subjects")
                .build()
        )
            .post(payload.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        val response = client.execute(request)

        return moshi.adapter<SearchSubjectsResponse>().lenient().fromJson(response) ?: throw RuntimeException()
    }

    fun getSubject(subjectId: Long): Subject {
        val seriesRequest = Request.Builder().url(
            apiV0Url.newBuilder().addPathSegments("subjects/$subjectId")
                .build()
        ).build()

        val response = client.execute(seriesRequest)

        return moshi.adapter<Subject>().fromJson(response) ?: throw RuntimeException()
    }

    fun getSubjectRelations(subjectId: Long): Collection<SubjectRelation> {
        val request = Request.Builder().url(
            apiV0Url.newBuilder().addPathSegments("subjects/$subjectId/subjects")
                .build()
        )
            .build()
        val response = client.execute(request)

        return moshi.adapter<Collection<SubjectRelation>>().lenient().fromJson(response) ?: throw RuntimeException()
    }

    fun getThumbnail(subject: Subject): Image? {
        return getThumbnail(subject.images.common ?: subject.images.medium)
    }

    fun getThumbnail(url: String): Image? {
        return url.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Image(bytes)
        }
    }
}
