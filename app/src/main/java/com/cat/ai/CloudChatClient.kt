package com.cat.ai

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI-compatible chat completions. The caller supplies the URL and key.
 */
class CloudChatClient {
    fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>>
    ): String {
        val root = baseUrl.trim().trimEnd('/')
        val endpoint = if (root.endsWith("/chat/completions")) root else "$root/chat/completions"
        val payload = JSONObject()
        payload.put("model", model.ifBlank { "llama3.2" })
        val array = JSONArray()
        messages.forEach { (role, content) ->
            array.put(JSONObject().put("role", role).put("content", content))
        }
        payload.put("messages", array)

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }
        try {
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                error("HTTP $code ${body.take(160)}")
            }
            val json = JSONObject(body)
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } finally {
            connection.disconnect()
        }
    }
}
