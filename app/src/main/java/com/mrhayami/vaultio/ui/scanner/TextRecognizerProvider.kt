package com.mrhayami.vaultio.ui.scanner

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Fail-once ML Kit client holder shared by live analysis and page-scan OCR. */
sealed interface TextRecognizerState {
    data class Available(val client: TextRecognizer) : TextRecognizerState
    data object Unavailable : TextRecognizerState
}

object TextRecognizerProvider {
    private val lock = Any()

    @Volatile
    private var state: TextRecognizerState? = null

    /** Creates the client at most once until [close]; failures are cached as [Unavailable]. */
    fun getOrCreate(): TextRecognizerState {
        state?.let { return it }
        synchronized(lock) {
            state?.let { return it }
            val created = try {
                TextRecognizerState.Available(
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                )
            } catch (_: Throwable) {
                TextRecognizerState.Unavailable
            }
            state = created
            return created
        }
    }

    fun close() {
        synchronized(lock) {
            val current = state
            if (current is TextRecognizerState.Available) {
                runCatching { current.client.close() }
            }
            state = null
        }
    }
}
