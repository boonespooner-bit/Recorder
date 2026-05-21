package com.recorder.app.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var recognizer: SpeechRecognizer? = null
    private var active = false

    private var pendingPartialResult: ((String) -> Unit)? = null
    private var pendingFinalResult: ((String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    var isTranscribing: Boolean = false
        private set

    fun startTranscribing(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        if (isTranscribing) return

        active = true
        isTranscribing = true
        pendingPartialResult = onPartialResult
        pendingFinalResult = onFinalResult

        mainHandler.post {
            startRecognizer(onPartialResult, onFinalResult)
        }
    }

    fun stopTranscribing() {
        active = false
        isTranscribing = false
        pendingPartialResult = null
        pendingFinalResult = null

        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
        }
    }

    private fun startRecognizer(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        recognizer?.destroy()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val results = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!results.isNullOrEmpty()) {
                    onPartialResult(results[0])
                }
            }

            override fun onResults(results: Bundle?) {
                val strings = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!strings.isNullOrEmpty()) {
                    onFinalResult(strings[0])
                }
            }

            override fun onEndOfSpeech() {
                if (active) {
                    mainHandler.post {
                        if (active) {
                            startRecognizer(onPartialResult, onFinalResult)
                        }
                    }
                }
            }

            override fun onError(error: Int) {
                if (active) {
                    mainHandler.postDelayed({
                        if (active) {
                            startRecognizer(onPartialResult, onFinalResult)
                        }
                    }, 500)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        sr.startListening(intent)
    }
}
