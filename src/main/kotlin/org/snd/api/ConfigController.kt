package org.snd.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.Context
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.snd.api.dto.AppConfigDto
import org.snd.api.dto.AppConfigUpdateDto
import org.snd.config.ConfigWriter
import org.snd.module.context.AppContext

class ConfigController(
    private val appContext: AppContext,
    private val configWriter: ConfigWriter,
    private val moshi: Moshi,
    private val configMapper: ConfigUpdateMapper,
) {

    fun register() {
        path("/") {
            get("config", this::getConfig)
            patch("config", this::updateConfig)
        }
    }

    private fun getConfig(ctx: Context): Context {
        val config = configMapper.toDto(appContext.appConfig)
        return ctx.result(moshi.adapter<AppConfigDto>().toJson(config))
            .contentType(APPLICATION_JSON)
            .status(OK)
    }

    private fun updateConfig(ctx: Context): Context {
        val request = moshi.adapter<AppConfigUpdateDto>().fromJson(ctx.body())
            ?: return ctx.status(BAD_REQUEST)

        val config = configMapper.patch(appContext.appConfig, request)
        try {
            appContext.configPath
                ?.let { configWriter.writeConfig(config, it) }
                ?: configWriter.writeConfigToDefaultPath(config)
        } catch (e: AccessDeniedException) {
            val response = moshi.adapter<Map<String, String?>>()
                .toJson(mapOf("message" to e.message))

            return ctx.result(response)
                .contentType(APPLICATION_JSON)
                .status(UNPROCESSABLE_CONTENT)
        }

        val thread = Thread { appContext.refresh() }
        thread.isDaemon = false
        thread.start()

        return ctx.status(NO_CONTENT)
    }
}