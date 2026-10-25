package com.example.musikku.data.remote.youtube

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Implementasi Downloader untuk NewPipeExtractor menggunakan OkHttpClient independen.
 * Menghandle request jaringan (GET, POST, HEAD) langsung dari perangkat Android,
 * mendukung cookie, serta meneruskan User-Agent dinamis (seperti VISIONOS / InnerTube).
 */
class NewPipeDownloader : Downloader() {

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:153.0) Gecko/20100101 Firefox/153.0"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE_KEY = "youtube_restricted_mode_key"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE = "PREF=f2=8000000"
        const val YOUTUBE_DOMAIN = "youtube.com"
    }

    private val cookies = ConcurrentHashMap<String, String>()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getCookies(url: String): String {
        val result = mutableListOf<String>()
        if (url.contains(YOUTUBE_DOMAIN)) {
            cookies[YOUTUBE_RESTRICTED_MODE_COOKIE_KEY]?.let { result.add(it) }
        }
        cookies["recaptcha_cookies"]?.let { result.add(it) }
        return result.joinToString("; ")
    }

    fun setCookie(key: String, cookie: String) {
        cookies[key] = cookie
    }

    fun removeCookie(key: String) {
        cookies.remove(key)
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpUrl = request.url()
        val httpMethod = request.httpMethod()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.let {
            val mimeType = headers["Content-Type"]?.firstOrNull() ?: "application/json; charset=utf-8"
            it.toRequestBody(mimeType.toMediaType())
        }

        val okRequestBuilder = okhttp3.Request.Builder()
            .url(httpUrl)
            .method(httpMethod, requestBody)
            .addHeader("User-Agent", DEFAULT_USER_AGENT)

        val cookieString = getCookies(httpUrl)
        if (cookieString.isNotEmpty()) {
            okRequestBuilder.addHeader("Cookie", cookieString)
        }

        // Teruskan header dari NewPipeExtractor.
        // Jika NewPipe menentukan header (seperti User-Agent khusus VISIONOS), ganti header default.
        for ((key, values) in headers) {
            okRequestBuilder.removeHeader(key)
            for (value in values) {
                okRequestBuilder.addHeader(key, value)
            }
        }

        val okResponse = client.newCall(okRequestBuilder.build()).execute()

        if (okResponse.code == 429) {
            okResponse.close()
            throw ReCaptchaException("reCaptcha Challenge requested", httpUrl)
        }

        var responseBodyToReturn: String? = null
        okResponse.body?.let { body ->
            try {
                responseBodyToReturn = body.string()
            } finally {
                body.close()
            }
        }

        val responseCode = okResponse.code
        val responseMessage = okResponse.message
        val responseHeaders = okResponse.headers.toMultimap()
        val latestUrl = okResponse.request.url.toString()

        return Response(responseCode, responseMessage, responseHeaders, responseBodyToReturn, latestUrl)
    }
}
