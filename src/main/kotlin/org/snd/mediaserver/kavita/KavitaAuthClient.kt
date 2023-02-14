package org.snd.mediaserver.kavita

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_BYTE_ARRAY
import org.snd.common.http.HttpClient
import org.snd.common.http.MEDIA_TYPE_JSON
import org.snd.mediaserver.kavita.model.KavitaAuthenticateResponse

class KavitaAuthClient(
    private val client: HttpClient,
    private val moshi: Moshi,
    private val baseUrl: HttpUrl,
) {

    fun authenticate(apiKey: String): KavitaAuthenticateResponse {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/plugin/authenticate")
                    .addQueryParameter("apiKey", apiKey)
                    .addQueryParameter("pluginName", "Komf")
                    .build()
            )
            .post(EMPTY_BYTE_ARRAY.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        val response = client.execute(request)

        return moshi.adapter<KavitaAuthenticateResponse>().fromJson(response) ?: throw RuntimeException()
    }

}