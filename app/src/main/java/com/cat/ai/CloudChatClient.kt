package com.cat.ai

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI-compatible chat completions. The caller supplies the URL and key.
 * Works with Google Gemini's OpenAI-compatible endpoint (CopilotPrefs.GEMINI_BASE_URL).
 */
class CloudChatClient {
    fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>>
    ): String {
        // Never reach emulator / loopback hosts from a real phone.
        require(!com.cat.data.CopilotPrefs.isLocalOnlyUrl(baseUrl)) { "local-only address" }
        require(com.cat.data.CopilotPrefs.looksLikeUrl(baseUrl)) { "not a web address" }
        val root = baseUrl.trim().trimEnd('/')
        val endpoint = if (root.endsWith("/chat/completions")) root else "$root/chat/completions"
        val payload = JSONObject()
        val chosen = com.cat.data.CopilotPrefs.modelFor(root, model)
        payload.put("model", chosen.ifBlank { "llama3.2" })
        val array = JSONArray()
        messages.forEach { (role, content) ->
            array.put(JSONObject().put("role", role).put("content", content))
        }
        payload.put("messages", array)

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            // Gemini Flash may think before answering; give it longer. Offline still covers failures.
            val gemini = com.cat.data.CopilotPrefs.isGeminiUrl(root)
            connectTimeout = if (gemini) 8_000 else 4_000
            readTimeout = if (gemini) 30_000 else 12_000
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
