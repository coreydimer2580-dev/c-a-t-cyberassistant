package com.cat.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Speaks assistant text with Android TTS. Prefers en-AU. Caps length.
 * Speak-only for Autopilot — not a microphone listener.
 */
class SpeechHelper(context: Context) {
    private val ready = AtomicBoolean(false)
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val au = Locale.forLanguageTag("en-AU")
                val result = engine.setLanguage(au)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.ENGLISH)
                }
                ready.set(true)
            }
        }
    }

    /** v1.16: Autopilot volume, 0.0–1.0. Applied per utterance; does not touch system volume. */
    @Volatile
    var volume: Float = 0.8f
        set(value) { field = value.coerceIn(0f, 1f) }

    fun speak(text: String, cap: Int = 400, flush: Boolean = true) {
        val engine = tts ?: return
        if (!ready.get()) return
        val clipped = text.replace(Regex("\\s+"), " ").trim().take(cap)
        if (clipped.isBlank()) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        if (volume <= 0f) return
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
        engine.speak(clipped, mode, params, "cat-autopilot-${System.nanoTime()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready.set(false)
    }
}
