package org.snd.common.http

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class SimpleCookieJar : CookieJar {
    private val cookieStore: MutableSet<Cookie> = ConcurrentHashMap.newKeySet()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cookieStore.removeIf { isExpired(it) }
        return cookieStore.filter { it.matches(url) }.toList()
    }

    private fun isExpired(cookie: Cookie): Boolean {
        return cookie.expiresAt < System.currentTimeMillis()
    }

}