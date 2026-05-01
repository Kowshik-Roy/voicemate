package com.example.voicemate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voicemate.CommandProcessor
import com.example.voicemate.MainActivity
import com.example.voicemate.R
import java.util.Locale

class BackgroundVoiceService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var tts: TextToSpeech? = null
    private val BENGALI_LOCALE = Locale("bn", "BD")
    private lateinit var audioManager: AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isWaitingForSearch = false
    private var searchTarget = ""

    companion object {
        var instance: BackgroundVoiceService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        tts = TextToSpeech(this, this)
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, createNotification())
        }
        startListening()
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                mainHandler.postDelayed({ restartListening() }, 500)
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""

                if (spokenText.isNotBlank()) {
                    // যদি কোনো কমান্ড পাওয়া যায় (যেমন বন্ধ করো, ছবি তোলো), তবে সার্চ ওয়েটিং মোড অফ করে দিবে
                    val isCommand = CommandProcessor.isACommand(spokenText)

                    if (isWaitingForSearch && !isCommand) {
                        handleSearchQuery(spokenText)
                    } else {
                        isWaitingForSearch = false
                        val response = CommandProcessor.processCommand(this@BackgroundVoiceService, spokenText)
                        
                        if (response.contains("কি সার্চ করতে হবে?")) {
                            isWaitingForSearch = true
                            searchTarget = if ("youtube" in spokenText || "ইউটিউব" in spokenText) "youtube" else "chrome"
                            speak(response, true) 
                        } else {
                            speak(response)
                            restartListening()
                        }
                    }
                } else {
                    restartListening()
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun handleSearchQuery(query: String) {
        isWaitingForSearch = false
        val encodedQuery = Uri.encode(query)
        val url = if (searchTarget == "youtube") {
            "https://www.youtube.com/results?search_query=$encodedQuery"
        } else {
            "https://www.google.com/search?q=$encodedQuery"
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            speak("আমি $query সার্চ করছি।")
        } catch (e: Exception) {
            speak("দুঃখিত, সার্চ করা সম্ভব হয়নি।")
        }
        
        searchTarget = ""
        mainHandler.postDelayed({ restartListening() }, 1000)
    }

    private fun startListening() {
        mainHandler.post {
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {}
        }
    }

    private fun restartListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                startListening()
            } catch (e: Exception) {}
        }
    }

    fun speak(text: String, startListeningAfter: Boolean = false) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.8).toInt(), 0)
        
        if (startListeningAfter) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post { speechRecognizer?.cancel() }
                }
                override fun onDone(utteranceId: String?) {
                    mainHandler.postDelayed({ startListening() }, 300)
                }
                override fun onError(utteranceId: String?) {
                    restartListening()
                }
            })
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VOICE_OUT")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "VOICE_OUT")
        } else {
            tts?.setOnUtteranceProgressListener(null)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VOICE_REPLY")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(BENGALI_LOCALE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "voice_service_channel")
            .setContentTitle("ভয়েস মেট চলছে")
            .setContentText("আমি আপনার কথা শুনছি...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "voice_service_channel", "Voice Mate Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        instance = null
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
