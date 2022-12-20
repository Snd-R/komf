package org.snd.metadata.providers.viz.model

import org.snd.metadata.model.BookRange
import java.time.LocalDate

data class VizBook(
    val id: VizBookId,
    val name: String,
    val seriesName: String,
    val number: BookRange?,
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
