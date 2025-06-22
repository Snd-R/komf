package snd.komf.providers.webtoons.model

import io.ktor.http.*
import kotlinx.serialization.Serializable
import snd.komf.providers.webtoons.WebtoonsClient

@Serializable
data class SearchApiResponse(
    val result: SearchResult,
    val success: Boolean
)

@Serializable
data class SearchResult(
    val challengeResult: ChallengeResult? = null,
    val webtoonResult: WebtoonResult? = null
)

@Serializable
data class ChallengeResult(
    val totalCount: Int,
    val titleList: List<Title>
)

@Serializable
data class WebtoonResult(
    val totalCount: Int,
    val titleList: List<Title>
)

@Serializable
data class Title(
    val titleNo: Int,
    val title: String,
    val titleGroupName: String?,
    val representGenre: String,
    val thumbnailMobile: String,
    val unsuitableForChildren: Boolean,
    val pictureAuthorName: String,
    val writingAuthorName: String,
    val lastEpisodeRegisterYmdt: Long,
    val readCount: Int
) {
    fun getOriginalUrl(): String {
        val titleGroupName = titleGroupName ?: seoEncoding(title)
        return "${WebtoonsClient.BASE_URL}/en/${representGenre}/${titleGroupName}/list?title_no=$titleNo"
    }

    fun getCanvasUrl(): String {
        val titleGroupName = titleGroupName ?: seoEncoding(title)
        return "${WebtoonsClient.BASE_URL}/en/canvas/${titleGroupName}/list?title_no=$titleNo"
    }

    fun getOriginalId(): WebtoonsSeriesId {
        return WebtoonsSeriesId(Url(getOriginalUrl()).encodedPathAndQuery)
    }

    fun getCanvasId(): WebtoonsSeriesId {
        return WebtoonsSeriesId(Url(getCanvasUrl()).encodedPathAndQuery)
    }
}

// From vendor-#####.js
private fun replaceEncodedChars(input: String): String {
    return input.replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}

// From vendor-#####.js
private fun seoEncoding(input: String): String {
    if (input.isEmpty()) {
        return "_"
    }

    var processedInput = replaceEncodedChars(input).lowercase()

    processedInput = processedInput.replace(Regex("[`~!@#$%^&*|\\\\'\";:/?\\$\\{\\$\\$\\$\\}]"), "")
        .replace(" ", "-")
        .replace("_", "-")
        .replace(Regex("-+"), "-")

    if (processedInput.startsWith("-")) {
        processedInput = processedInput.substring(1)
    }

    return processedInput.ifEmpty { "_" }
}