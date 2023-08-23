package org.snd.common.http

import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level

private val logger = KotlinLogging.logger {}

class LoggingInterceptor(level: Level = Level.BODY) : Interceptor {

    private val realInterceptor = HttpLoggingInterceptor { message -> logger.debug { message } }
        .setLevel(level)


    override fun intercept(chain: Interceptor.Chain) = realInterceptor.intercept(chain)
}

