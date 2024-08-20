package snd.komf.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import snd.komf.api.notifications.KomfAppriseRenderResult
import snd.komf.api.notifications.KomfAppriseRequest
import snd.komf.api.notifications.KomfAppriseTemplates
import snd.komf.api.notifications.KomfDiscordTemplates
import snd.komf.api.notifications.KomfDiscordRenderResult
import snd.komf.api.notifications.KomfDiscordRequest

class KomfNotificationClient(
    private val ktor: HttpClient,
) {
    suspend fun getDiscordTemplates(): KomfDiscordTemplates {
        return ktor.get("/api/notifications/discord/templates").body()
    }

    suspend fun updateDiscordTemplates(templates: KomfDiscordTemplates): KomfDiscordTemplates {
        return ktor.post("/api/notifications/discord/templates") {
            contentType(ContentType.Application.Json)
            setBody(templates)
        }.body()
    }

    suspend fun renderDiscord(request: KomfDiscordRequest): KomfDiscordRenderResult {
        return ktor.post("/api/notifications/discord/render") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun sendDiscord(request: KomfDiscordRequest) {
        ktor.post("/api/notifications/discord/send") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }


    suspend fun getAppriseTemplates(): KomfAppriseTemplates {
        return ktor.get("/api/notifications/apprise/templates").body()
    }

    suspend fun updateAppriseTemplates(templates: KomfAppriseTemplates): KomfAppriseTemplates {
        return ktor.post("/api/notifications/apprise/templates") {
            contentType(ContentType.Application.Json)
            setBody(templates)
        }.body()
    }

    suspend fun renderApprise(request: KomfAppriseRequest): KomfAppriseRenderResult {
        return ktor.post("/api/notifications/apprise/render") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun sendApprise(request: KomfAppriseRequest) {
        ktor.post("/api/notifications/apprise/send") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

}