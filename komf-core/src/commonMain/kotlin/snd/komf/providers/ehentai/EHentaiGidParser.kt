package snd.komf.providers.ehentai

import snd.komf.model.ProviderSeriesId

fun ProviderSeriesId.parseEHentaiGid(): Pair<Int, String> {
    val parts = this.value.split(";", limit = 2)

    require(parts.size == 2) { "Invalid E-Hentai ID format: ${this.value}" }
    val gid = parts[0].toIntOrNull()
        ?: throw IllegalArgumentException("Invalid GID (Not a number) in ID: ${this.value}")
    val token = parts[1]

    return Pair(gid, token)
}