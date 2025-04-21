package com.example.securekey.utilities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.Locale

class SpeechRecognitionManager(
    private val context: Context,
    private val onLockStatusChanged: (Int, Boolean) -> Unit
) {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private val RECORD_AUDIO_PERMISSION_CODE = 101
    private var isListening = false
    private val TAG = "SpeechRecognitionManager"

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            initializeSpeechRecognizer()
        } else {
            Toast.makeText(
                context,
                "Speech recognition is not available on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("iw", "IL")) // Hebrew language
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech begun")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // Restart listening after a short delay
                if (isListening) {
                    (context as? FragmentActivity)?.runOnUiThread {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isListening) {
                                speechRecognizer.startListening(recognizerIntent)
                            }
                        }, 1000)
                    }
                }
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")

                if (error != SpeechRecognizer.ERROR_NO_MATCH && isListening) {
                    (context as? FragmentActivity)?.runOnUiThread {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isListening) {
                                speechRecognizer.startListening(recognizerIntent)
                            }
                        }, 1000)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                processVoiceResults(matches)
                if (isListening) {
                    speechRecognizer.startListening(recognizerIntent)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                processVoiceResults(matches)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun processVoiceResults(matches: ArrayList<String>?) {
        matches?.let {
            if (it.isNotEmpty()) {
                val phrase = it[0].lowercase(Locale.getDefault())
                Log.d(TAG, "Heard phrase: $phrase")

                if (phrase.contains("בבקשה תפתח")) {
                    Log.d(TAG, "Magic phrase detected! Unlocking...")
                    (context as? FragmentActivity)?.runOnUiThread {
                        onLockStatusChanged(1, true)
                    }
                }
            }
        }
    }

    fun startListening() {
        if (checkAudioPermission()) {
            isListening = true
            speechRecognizer.startListening(recognizerIntent)
            Log.d(TAG, "Started listening")
        } else {
            requestAudioPermission()
        }
    }

    fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
        Log.d(TAG, "Stopped listening")
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        Log.d(TAG, "Requesting audio permission")
        (context as? FragmentActivity)?.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Audio permission granted, starting to listen")
                    startListening()
                } else {
                    Log.e(TAG, "Audio permission denied")
                    Toast.makeText(
                        context,
                        "Microphone permission is required for voice recognition.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}