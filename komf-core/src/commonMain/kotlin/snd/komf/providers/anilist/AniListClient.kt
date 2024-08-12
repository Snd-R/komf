package snd.komf.providers.anilist

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import snd.komf.model.Image
import snd.komf.providers.anilist.model.AniListMedia
import snd.komf.providers.anilist.model.AniListMediaFormat
import snd.komf.providers.anilist.model.AniListMediaQuery
import snd.komf.providers.anilist.model.AniListMediaResponse
import snd.komf.providers.anilist.model.AniListMediaSearchResponse
import snd.komf.providers.anilist.model.AniListResponse
import snd.komf.providers.anilist.model.AniListSearchQuery

private const val graphqlUrl = "https://graphql.anilist.co"

private const val mangaFragment = """
    id
    type,
    format,
    title {
        romaji
        english
        native
    },
    status,
    description(asHtml: false),
    chapters,
    volumes,
    coverImage {
        extraLarge
    },
    startDate {
        year,
        month,
        day
    }
    genres,
    synonyms,
    tags {
        name,
        description,
        category,
        rank
    },
    staff {
        edges {
            node {
                name {
                    full
                },
                languageV2
            }
            role
        }
    },
    meanScore
"""

private const val searchQuery = """
query Search(${"\$search"}: String!, ${"\$perPage"}: Int!, ${"\$formats"}:[MediaFormat!]! ) {
    mediaSearch: Page(page: 1, perPage: ${"\$perPage"}) {
        media(type: MANGA, format_in: ${"\$formats"}, search: ${"\$search"}){
            $mangaFragment
        }
    }
}
"""
private const val seriesQuery = """
query Media(${"\$id"}: Int!){
    media: Media(id: ${"\$id"}) {
            $mangaFragment
    }
}
"""


class AniListClient(
    private val ktor: HttpClient,
) {
    suspend fun search(name: String, formats: List<AniListMediaFormat>, pageSize: Int = 10): List<AniListMedia> {
        val response: AniListResponse<AniListMediaSearchResponse> = ktor.post(graphqlUrl) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                AniListGraphQLRequest(
                    query = searchQuery,
                    variables = AniListSearchQuery(
                        search = name,
                        formats = formats,
                        perPage = pageSize
                    )
                )
            )
        }.body()

        return response.data.mediaSearch.media
    }

    suspend fun getMedia(id: Int): AniListMedia {
        val response: AniListResponse<AniListMediaResponse> = ktor.post(graphqlUrl) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(
                AniListGraphQLRequest(
                    query = seriesQuery,
                    variables = AniListMediaQuery(id = id)
                )
            )
        }.body()

        return response.data.media
    }

    suspend fun getThumbnail(series: AniListMedia): Image? {
        return series.coverImage?.extraLarge?.let {
            val bytes: ByteArray = ktor.get(it).body()
            Image(bytes)
        }
    }

    @Serializable
    private data class AniListGraphQLRequest<Q, T>(
        val query: Q,
        val variables: T
    )
}
