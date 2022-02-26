package org.snd.komga.model

import com.squareup.moshi.JsonClass
import java.net.URI

@JsonClass(generateAdapter = true)
data class WebLink(
  val label: String,
  val url: URI,
)
