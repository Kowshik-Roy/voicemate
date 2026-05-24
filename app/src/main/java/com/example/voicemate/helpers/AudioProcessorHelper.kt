package com.example.voicemate.helpers

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor

object AudioProcessorHelper {

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    /**
     * মাইক্রোফোনের নয়েজ রিডাকশন এবং গেইন কন্ট্রোল চালু করে।
     * এটি SpeechRecognizer এর ইনপুট কোয়ালিটি অনেক বাড়িয়ে দেয়।
     */
    @SuppressLint("MissingPermission")
    fun startEnhancing() {
        try {
            val sampleRate = 16000
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            // VOICE_RECOGNITION সোর্স ব্যবহার করা হয়েছে যা স্বয়ংক্রিয়ভাবে ভয়েস ক্ল্যারিটি বাড়ায়
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize
            )

            val sessionId = audioRecord?.audioSessionId ?: return

            // নয়েজ সাপ্রেসর চালু করা (যদি ডিভাইসে থাকে)
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)
                noiseSuppressor?.enabled = true
            }

            // কথা কম সাউন্ডে হলে অটোমেটিক সাউন্ড বাড়িয়ে দিবে
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId)
                agc?.enabled = true
            }

            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopEnhancing() {
        try {
            noiseSuppressor?.release()
            agc?.release()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
