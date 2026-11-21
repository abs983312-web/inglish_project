package uz.inglish.app.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Small [TextToSpeech] wrapper that keeps the last requested text until the
 * engine finishes initialising, so the first tap on "Tinglash" is not lost.
 */
class TextToSpeechController(context: Context) {
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            val speech = engine
            if (status == TextToSpeech.SUCCESS && speech != null) {
                var result = speech.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Fall back to US English if the UK voice is not installed.
                    result = speech.setLanguage(Locale.US)
                }
                ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                pendingText?.let { text ->
                    pendingText = null
                    if (ready) speak(text)
                }
            }
        }
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (!ready) {
            pendingText = clean
            return
        }
        engine?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        pendingText = null
        engine?.stop()
    }

    fun shutdown() {
        pendingText = null
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private companion object {
        const val UTTERANCE_ID = "inglish-speaking-prompt"
    }
}
