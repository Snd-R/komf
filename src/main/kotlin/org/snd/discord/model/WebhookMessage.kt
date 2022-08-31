package org.snd.discord.model

import org.apache.velocity.VelocityContext

data class WebhookMessage(
    val library: Library,
    val series: Series,
    val books: Collection<Book>
)

data class Library(val name: String)

data class Series(
    val id: String,
    val name: String,
    val summary: String,
)

data class Book(
    val id: String,
    val name: String,
)

fun WebhookMessage.toVelocityContext(): VelocityContext {
    val context = VelocityContext()
    context.put("library", library)
    context.put("series", series)
    context.put("books", books.sortedBy { it.name })
    return context
}
