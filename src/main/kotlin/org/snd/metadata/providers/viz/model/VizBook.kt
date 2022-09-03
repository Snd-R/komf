package org.snd.metadata.providers.viz.model

import java.time.LocalDate

data class VizBook(
    val id: VizBookId,
    val name: String,
    val seriesName: String,
    val number: Int?,
    val publisher: String = "Viz",
    val releaseDate: LocalDate?,
    val description: String?,
    val coverUrl: String?,
    val genres: Collection<String>,
    val isbn: String?,
    val ageRating: AgeRating?,
    val authorStory: String?,
    val authorArt: String?,

    val allBooksId: VizAllBooksId,
)
