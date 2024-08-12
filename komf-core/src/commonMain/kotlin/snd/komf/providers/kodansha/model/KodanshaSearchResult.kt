package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders

@Serializable
data class KodanshaSearchResult(
    val type: String,
    val displayType: String,
    val content: KodanshaSearchResultContent,
)

@Serializable
data class KodanshaSearchResultContent(
    val id: Int,
    val seriesName: String?,
    val title: String,
    val description: String?,
    val thumbnails: List<KodanshaThumbnail>,
    val readableUrl: String?,
)


