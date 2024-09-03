package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable

@Serializable
data class KodanshaSearchResult(
    val type: String,
    val displayType: String,
    val content: KodanshaSearchResultContent,
)

@Serializable
data class KodanshaSearchResultContent(
    val id: Int,
    val title: String,
    val seriesName: String? = null,
    val description: String? = null,
    val thumbnails: List<KodanshaThumbnail> = emptyList(),
    val readableUrl: String? = null,
)
