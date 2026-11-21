package uz.inglish.app.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper around the platform [SpeechRecognizer].
 *
 * Speech recognition stays on the device (or the system provider), so no audio
 * ever reaches our backend: only the resulting transcript is submitted, exactly
 * like text typed by the learner.
 */
class SpeechRecognizerController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var cancelled = false
    private var partialHandler: ((String) -> Unit)? = null
    private var resultHandler: ((String) -> Unit)? = null
    private var errorHandler: ((String) -> Unit)? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        stop()

        if (!isAvailable) {
            onError(
                "Bu qurilmada nutqni aniqlash xizmati topilmadi. " +
                    "Javobni matn ko'rinishida kiriting.",
            )
            return
        }

        cancelled = false
        partialHandler = onPartial
        resultHandler = onResult
        errorHandler = onError

        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speech
        speech.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    if (cancelled) return
                    val handler = errorHandler
                    release()
                    handler?.invoke(errorMessage(error))
                }

                override fun onResults(results: Bundle?) {
                    if (cancelled) return
                    val text = firstResult(results)
                    val handler = resultHandler
                    val onFailure = errorHandler
                    release()
                    if (text.isNotEmpty()) {
                        handler?.invoke(text)
                    } else {
                        onFailure?.invoke("Ovoz eshitilmadi. Qayta urinib ko'ring.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (cancelled) return
                    val text = firstResult(partialResults)
                    if (text.isNotEmpty()) partialHandler?.invoke(text)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
        }

        runCatching { speech.startListening(intent) }
            .onFailure {
                val handler = errorHandler
                release()
                handler?.invoke("Mikrofonni ishga tushirib bo'lmadi. Qayta urinib ko'ring.")
            }
    }

    /** Cancels an in-flight session without surfacing an error to the UI. */
    fun stop() {
        cancelled = true
        recognizer?.let { speech ->
            runCatching { speech.stopListening() }
            runCatching { speech.cancel() }
        }
        release()
    }

    fun destroy() = stop()

    private fun release() {
        recognizer?.let { speech -> runCatching { speech.destroy() } }
        recognizer = null
        partialHandler = null
        resultHandler = null
        errorHandler = null
    }

    private fun firstResult(bundle: Bundle?): String =
        bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Mikrofon xatosi. Qayta urinib ko'ring."
        SpeechRecognizer.ERROR_CLIENT -> "Nutqni aniqlash to'xtatildi."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon ruxsati berilmagan."
        SpeechRecognizer.ERROR_NETWORK -> "Internet aloqasi yo'q. Nutqni aniqlash uchun internet kerak."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Internet sekin. Qayta urinib ko'ring."
        SpeechRecognizer.ERROR_NO_MATCH -> "Ovoz tushunilmadi. Aniqroq gapirib ko'ring."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Nutq xizmati band. Bir ozdan keyin urinib ko'ring."
        SpeechRecognizer.ERROR_SERVER -> "Nutq serverida xatolik. Qayta urinib ko'ring."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ovoz eshitilmadi. Qayta urinib ko'ring."
        else -> "Nutqni aniqlab bo'lmadi. Qayta urinib ko'ring."
    }
}
