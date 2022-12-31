package org.snd.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.OK
import org.snd.config.AppConfig
import org.snd.config.ConfigWriter
import org.snd.module.AppContext

class ConfigController(
    private val appContext: AppContext,
    private val config: AppConfig,
    private val configWriter: ConfigWriter,
    private val moshi: Moshi,
    private val configMapper: ConfigUpdateMapper,
) {

    fun register() {
        path("/") {
            patch("config", this::config)
        }
    }

    private fun config(ctx: Context): Context {
        val request = moshi.adapter<ConfigUpdateRequest>().fromJson(ctx.body())
            ?: return ctx.status(BAD_REQUEST)

        val config = configMapper.patch(config, request)
        appContext.configPath
            ?.let { configWriter.writeConfig(config, it) }
            ?: configWriter.writeConfigToDefaultPath(config)

        val thread = Thread { performRefresh() }
        thread.start()

        return ctx.status(OK)
    }

    private fun performRefresh() {
        Thread.sleep(200L)
        appContext.refresh()
    }
}