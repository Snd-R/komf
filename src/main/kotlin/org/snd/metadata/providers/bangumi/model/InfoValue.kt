package org.snd.metadata.providers.bangumi.model

data class InfoValue (
    val rawString: String,
    val list: List<Map<String, String>>?
)