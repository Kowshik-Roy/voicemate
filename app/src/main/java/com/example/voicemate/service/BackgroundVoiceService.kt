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
    
    // রিকগনাইজার আটকে গেলে তা উদ্ধারের জন্য ওয়াচডগ
    private val watchdogRunnable = Runnable {
        if (!isSpeaking && !isListening) {
            Log.d("VoiceMate", "Watchdog triggered: Restarting listening...")
            restartListening()
        }
    }

    companion object {
        var instance: BackgroundVoiceService? = null
        const val ACTION_LISTENING_STATE = "com.example.voicemate.LISTENING_STATE"
        const val EXTRA_IS_LISTENING = "is_listening"
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
            } else { 0 }
            startForeground(1, notification, type)
        } else {
            startForeground(1, notification)
        }
        startListening()
        return START_STICKY
    }

    private fun sendListeningState(listening: Boolean) {
        val intent = Intent(ACTION_LISTENING_STATE)
        intent.putExtra(EXTRA_IS_LISTENING, listening)
        sendBroadcast(intent)
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        sendListeningState(true)
                        updateNotification(if (isTypingMode) "আমি লিখছি... বলুন" else "আমি শুনছি...")
                        mainHandler.removeCallbacks(watchdogRunnable)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                        sendListeningState(false)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        sendListeningState(false)
                        Log.e("VoiceMate", "Speech Error: $error")
                        
                        if (!isSpeaking) {
                            val delay = when (error) {
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1000L
                                SpeechRecognizer.ERROR_NO_MATCH -> 300L
                                else -> 1000L
                            }
                            // Busy এরর হলে রিকগনাইজার রি-ইনিশিয়ালাইজ করা জরুরি
                            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                                mainHandler.postDelayed({ 
                                    initSpeechRecognizer()
                                    startListening() 
                                }, delay)
                            } else {
                                mainHandler.postDelayed({ startListening() }, delay)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        sendListeningState(false)
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
            } catch (e: Exception) {
                Log.e("VoiceMate", "SR Init Error: ${e.message}")
            }
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("bn-BD", "en-US"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
    }

    private fun processResults(matches: ArrayList<String>) {
        var handled = false
        
        for (spokenText in matches) {
            val spokenTextLower = spokenText.lowercase(Locale.getDefault())

            if (isTypingMode) {
                if (listOf("বন্ধ", "কাটো", "exit", "stop typing").any { spokenTextLower.contains(it) }) {
                    isTypingMode = false
                    speak("টাইপিং মোড বন্ধ করা হয়েছে।", true)
                    handled = true
                    break
                }
                
                val service = VoiceAccessibilityService.instance
                if (service != null) {
                    if (spokenTextLower == "পাঠাও" || spokenTextLower == "send") {
                        service.clickSend()
                        speak("মেসেজ পাঠানো হয়েছে", true)
                    } else {
                        val success = service.appendText(spokenText)
                        if (spokenTextLower.endsWith(" পাঠাও") || spokenTextLower.endsWith(" send")) {
                            service.clickSend()
                            speak("পাঠানো হয়েছে", true)
                        } else {
                            // টাইপিং মোডে দ্রুত পরবর্তী কথা শোনার জন্য
                            mainHandler.postDelayed({ restartListening() }, 400)
                        }
                    }
                    handled = true
                    break
                }
            }

            if (isWaitingForSearch) {
                handleSearchQuery(spokenText)
                handled = true
                break
            }

            val isCommand = CommandProcessor.isACommand(spokenText)
            val isGreeting = CommandProcessor.isGreeting(spokenText)

            if (isCommand || isGreeting) {
                val response = CommandProcessor.processCommand(this, spokenText)
                when {
                    response == "ACTION_START_TYPING" -> {
                        if (VoiceAccessibilityService.instance == null) {
                            speak("টাইপিং শুরু করতে অ্যাক্সেসিবিলিটি সার্ভিস অন করুন।", false)
                        } else {
                            isTypingMode = true
                            VoiceAccessibilityService.instance?.focusAndOpenKeyboard()
                            speak("টাইপিং মোড চালু হয়েছে। কি লিখবো বলুন?", true)
                        }
                    }
                    response.startsWith("PROMPT_SEARCH|") -> {
                        val parts = response.split("|")
                        searchTarget = parts[1]
                        isWaitingForSearch = true
                        speak(parts[2], true)
                    }
                    response != "UNKNOWN_COMMAND" -> {
                        speak(response, true)
                    }
                    else -> continue
                }
                handled = true
                break
            }
        }

        if (!handled) restartListening()
    }

    private fun handleSearchQuery(query: String) {
        isWaitingForSearch = false
        val encodedQuery = Uri.encode(query)
        val url = if (searchTarget == "youtube") "https://www.youtube.com/results?search_query=$encodedQuery"
                  else "https://www.google.com/search?q=$encodedQuery"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            speak("আমি $query সার্চ করছি।", true)
        } catch (_: Exception) { speak("সার্চ করা যায়নি।", true) }
        searchTarget = ""
    }

    private fun startListening() {
        if (isListening || isSpeaking) return
        mainHandler.post {
            try {
                speechRecognizer?.startListening(recognizerIntent)
                // ৫ সেকেন্ডের মধ্যে কথা না শুনলে রিস্টার্ট করার ব্যবস্থা
                mainHandler.postDelayed(watchdogRunnable, 6000)
            } catch (e: Exception) {
                restartListening()
            }
        }
    }

    private fun restartListening() {
        mainHandler.removeCallbacks(watchdogRunnable)
        if (isSpeaking) return
        
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                isListening = false
                // পুরোপুরি নতুনভাবে রিকগনাইজার চালু করা যাতে Busy এরর না আসে
                initSpeechRecognizer()
                mainHandler.postDelayed({ startListening() }, 400)
            } catch (e: Exception) {
                initSpeechRecognizer()
                startListening()
            }
        }
    }

    fun speak(text: String, startListeningAfter: Boolean = true) {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "VOICE_REPLY")
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                mainHandler.post { 
                    speechRecognizer?.cancel()
                    isListening = false
                    sendListeningState(false)
                }
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                if (startListeningAfter) {
                    // কথা শেষ হওয়ার পর একটি বিরতি দিয়ে রিকগনাইজার রিস্টার্ট
                    mainHandler.postDelayed({ restartListening() }, 700)
                }
            }
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                if (startListeningAfter) restartListening()
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                isSpeaking = false
                if (startListeningAfter) restartListening()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "VOICE_REPLY")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = bengaliLocale
        }
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, createNotification(status))
    }

    private fun createNotification(contentText: String): Notification {
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "voice_service_channel")
            .setContentTitle("ভয়েস মেট")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("voice_service_channel", "Voice Mate", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
