package com.cat.ai

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

/**
 * Optional public HTTPS lookup for Online evolve.
 * Uses Wikipedia REST summary (and DuckDuckGo Instant Answer as fallback).
 * Does NOT read browser history, OneDrive, SMS, or other apps.
 */
class OnlineEvolveClient {
    data class Hit(val note: String, val source: String)

    fun lookup(topicKeywords: String): Hit? {
        val query = topicKeywords.trim().replace(Regex("\\s+"), " ").take(80)
        if (query.length < 2) return null
        return wikiSummary(query) ?: duckDuckGo(query)
    }

    private fun wikiSummary(query: String): Hit? {
        val title = URLEncoder.encode(query.replace(' ', '_'), Charsets.UTF_8.name())
        val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$title"
        val body = get(url) ?: return null
        return runCatching {
            val json = JSONObject(body)
            if (json.optString("type") == "disambiguation") return null
            val extract = json.optString("extract").trim()
            if (extract.isBlank()) return null
            val titleLabel = json.optString("title").ifBlank { query }
            Hit(
                note = "online: $titleLabel — ${extract.take(160)}",
                source = "wikipedia"
            )
        }.getOrNull()
    }

    private fun duckDuckGo(query: String): Hit? {
        val q = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = "https://api.duckduckgo.com/?q=$q&format=json&no_html=1&skip_disambig=1"
        val body = get(url) ?: return null
        return runCatching {
            val json = JSONObject(body)
            val abstract = json.optString("AbstractText").trim()
            val heading = json.optString("Heading").ifBlank { query }
            if (abstract.isBlank()) {
                val related = json.optJSONArray("RelatedTopics")
                if (related != null && related.length() > 0) {
                    val first = related.optJSONObject(0) ?: return null
                    val text = first.optString("Text").trim()
                    if (text.isBlank()) return null
                    return Hit(note = "online: ${text.take(180)}", source = "duckduckgo")
                }
                return null
            }
            Hit(note = "online: $heading — ${abstract.take(160)}", source = "duckduckgo")
        }.getOrNull()
    }

    private fun get(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3_500
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "C@T-CyberAssistant/1.8 (offline-first; public-lookup)")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code in 200..299) text else null
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
