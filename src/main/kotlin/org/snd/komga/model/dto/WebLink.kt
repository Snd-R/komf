package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass
import java.net.URI

@JsonClass(generateAdapter = true)
data class WebLink(
  val label: String,
  val url: URI,
)
