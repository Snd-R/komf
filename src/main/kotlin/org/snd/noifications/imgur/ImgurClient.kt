package org.snd.noifications.imgur

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.common.http.HttpClient
import org.snd.noifications.imgur.model.ImgurCredits
import org.snd.noifications.imgur.model.ImgurImage
import org.snd.noifications.imgur.model.ImgurResponse

class ImgurClient(
    private val client: HttpClient,
    private val moshi: Moshi
) {
    private val baseUrl = "https://api.imgur.com/3/".toHttpUrl()

    fun uploadImage(image: ByteArray): ImgurResponse<ImgurImage> {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", "image", image.toRequestBody())
            .build()
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("image").build())
            .post(requestBody)
            .build()
        val response = client.execute(request)

        return moshi.adapter<ImgurResponse<ImgurImage>>()
            .fromJson(response) ?: throw RuntimeException()
    }

    fun getCredits(): ImgurResponse<ImgurCredits> {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("credits").build())
            .get()
            .build()

        val response = client.execute(request)

        return moshi.adapter<ImgurResponse<ImgurCredits>>()
            .fromJson(response) ?: throw RuntimeException()
    }
}