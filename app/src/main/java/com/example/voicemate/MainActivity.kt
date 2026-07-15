package com.example.voicemate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.airbnb.lottie.LottieAnimationView
import com.example.voicemate.service.BackgroundVoiceService

class MainActivity : AppCompatActivity() {

    private lateinit var lottieAnimation: LottieAnimationView
    private lateinit var tvStatus: TextView

    private val listeningStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isListening = intent?.getBooleanExtra(BackgroundVoiceService.EXTRA_IS_LISTENING, false) ?: false
            updateUIState(isListening)
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
                startVoiceService()
            } else {
                Toast.makeText(this, "মাইক্রোফোন পারমিশন ছাড়া অ্যাপটি কাজ করবে না", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lottieAnimation = findViewById(R.id.lottieAnimation)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopVoiceService()
            updateUIState(false)
            Toast.makeText(this, "সার্ভিস বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnExitApp).setOnClickListener {
            stopVoiceService()
            finishAffinity()
        }

        findViewById<Button>(R.id.btnOpenAccessibility).setOnClickListener {
            openPermissionSettings()
        }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateUIState(isListening: Boolean) {
        if (isListening) {
            lottieAnimation.playAnimation()
            tvStatus.text = "আমি আপনার কথা শুনছি..."
        } else {
            lottieAnimation.pauseAnimation()
            tvStatus.text = "অ্যাসিস্ট্যান্ট আপনার কথা শোনার জন্য তৈরি..."
        }
    }

    private fun openPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            startActivity(intent)
            Toast.makeText(this, "প্রথমে 'Display over other apps' অন করুন", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Installed Services থেকে 'Voice Mate' অন করুন", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "সেটিংস খুঁজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BackgroundVoiceService.ACTION_LISTENING_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(listeningStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(listeningStateReceiver, filter)
        }
        requestNeededPermissions()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(listeningStateReceiver)
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            startVoiceService()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun startVoiceService() {
        val intent = Intent(this, BackgroundVoiceService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopVoiceService() {
        stopService(Intent(this, BackgroundVoiceService::class.java))
    }
}
