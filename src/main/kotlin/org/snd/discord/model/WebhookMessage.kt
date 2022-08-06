package org.snd.discord.model

import org.apache.velocity.VelocityContext

data class WebhookMessage(
    val libraryName: String,
    val seriesName: String,
    val seriesSummary: String,
    val books: Collection<String>
)

fun WebhookMessage.toVelocityContext(): VelocityContext {
    val context = VelocityContext()
    context.put("libraryName", libraryName)
    context.put("seriesName", seriesName)
    context.put("seriesSummary", seriesSummary)
    context.put("books", books)
    return context
}
