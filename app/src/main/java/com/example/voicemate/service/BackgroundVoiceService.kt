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
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.voicemate.CommandProcessor
import com.example.voicemate.MainActivity
import com.example.voicemate.R
import java.util.Locale

class BackgroundVoiceService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var tts: TextToSpeech? = null
    private val bengaliLocale = Locale("bn", "BD")
    private lateinit var audioManager: AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isWaitingForSearch = false
    private var searchTarget = ""
    private var isTypingMode = false
    private var isListening = false
    private var isSpeaking = false

    companion object {
        var instance: BackgroundVoiceService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        tts = TextToSpeech(this, this)
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("ভয়েস মেট সক্রিয় আছে")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
            startForeground(1, notification, type)
        } else {
            startForeground(1, notification)
        }
        
        startListening()
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        updateNotification("আমি আপনার কথা শুনছি...")
                        Toast.makeText(applicationContext, "🔴 আমি শুনছি... বলুন", Toast.LENGTH_SHORT).show()
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                        updateNotification("প্রসেসিং হচ্ছে...")
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        if (!isSpeaking) {
                            mainHandler.postDelayed({ startListening() }, 1000)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        if (matches.isNotEmpty()) {
                            processResults(matches)
                        } else {
                            restartListening()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            } catch (_: Exception) {
                Log.e("VoiceMate", "Initialization failed")
            }
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
    }

    private fun processResults(matches: ArrayList<String>) {
        var handled = false
        val firstResult = matches[0]

        for (spokenText in matches) {
            val spokenTextLower = spokenText.lowercase(Locale.getDefault())
            
            if (isTypingMode) {
                if (spokenTextLower.contains("টাইপিং বন্ধ") || spokenTextLower.contains("stop typing")) {
                    isTypingMode = false
                    speak("টাইপিং মোড বন্ধ করা হয়েছে।", true)
                } else {
                    VoiceAccessibilityService.instance?.appendText(spokenText)
                    restartListening()
                }
                handled = true
                break
            }

            val isCommand = CommandProcessor.isACommand(spokenTextLower)
            val isGreeting = CommandProcessor.isGreeting(spokenTextLower)

            if (isWaitingForSearch && !isCommand && !isGreeting) {
                handleSearchQuery(spokenText)
                handled = true
                break
            } else if (isCommand || isGreeting) {
                isWaitingForSearch = false
                val response = CommandProcessor.processCommand(this, spokenText)
                
                when {
                    response.contains("কি সার্চ করতে হবে?") -> {
                        isWaitingForSearch = true
                        searchTarget = if ("youtube" in spokenTextLower || "ইউটিউব" in spokenTextLower) "youtube" else "chrome"
                        speak(response, true) 
                    }
                    response.contains("টাইপিং মোড চালু") -> {
                        isTypingMode = true
                        VoiceAccessibilityService.instance?.focusAndOpenKeyboard()
                        speak(response, true)
                    }
                    else -> {
                        speak(response, true)
                    }
                }
                handled = true
                break
            }
        }
        
        if (!handled) {
            val fallbackResponse = CommandProcessor.processCommand(this, firstResult)
            if (!fallbackResponse.contains("বুঝতে পারছি না")) {
                speak(fallbackResponse, true)
            } else {
                restartListening()
            }
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
            speak("আমি $query সার্চ করছি।", true)
        } catch (_: Exception) {
            speak("দুঃখিত, সার্চ করা সম্ভব হয়নি।", true)
        }
        searchTarget = ""
    }

    private fun startListening() {
        if (isListening || isSpeaking) return
        mainHandler.post {
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (_: Exception) {
                restartListening()
            }
        }
    }

    private fun restartListening() {
        if (isSpeaking) return
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                isListening = false
                startListening()
            } catch (_: Exception) {
                initSpeechRecognizer()
                startListening()
            }
        }
    }

    fun speak(text: String, startListeningAfter: Boolean = true) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.8).toInt(), 0)
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                mainHandler.post { 
                    speechRecognizer?.cancel() 
                    isListening = false
                    updateNotification("অ্যাসিস্ট্যান্ট কথা বলছে...")
                }
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                if (startListeningAfter) {
                    mainHandler.postDelayed({ startListening() }, 600)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                if (startListeningAfter) restartListening()
            }
        })

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VOICE_REPLY")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "VOICE_REPLY")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = bengaliLocale
        }
    }

    private fun updateNotification(status: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, createNotification(status))
    }

    private fun createNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "voice_service_channel")
            .setContentTitle("ভয়েস মেট")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
        speechRecognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
