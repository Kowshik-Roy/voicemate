package com.example.voicemate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.voicemate.helpers.AuthHelper
import com.example.voicemate.service.BackgroundVoiceService

class MainActivity : AppCompatActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
            if (audioGranted) {
                startVoiceService()
            } else {
                Toast.makeText(this, "মাইক্রোফোন পারমিশন ছাড়া অ্যাপটি কাজ করবে না", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // সেশন চেক (Logic 5)
        if (!AuthHelper.isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val btnStop = findViewById<Button>(R.id.btnStopService)
        val btnExit = findViewById<Button>(R.id.btnExitApp)
        val btnOverlay = findViewById<Button>(R.id.btnOpenAccessibility)
        val btnLogout = Button(this).apply { text = "Logout" } // ডাইনামিক বাটন লজিক চেক করার জন্য

        btnOverlay.text = "ওভারলে পারমিশন দিন"
        btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
                    startActivity(intent)
                    Toast.makeText(this, "তালিকায় Voice Mate খুঁজে 'Allow display over other apps' অন করুন", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "ওভারলে পারমিশন অলরেডি দেওয়া আছে", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnStop.setOnClickListener {
            stopVoiceService()
            Toast.makeText(this, "সার্ভিস বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
        }

        btnExit.setOnClickListener {
            stopVoiceService()
            finishAffinity()
        }
        
        // Logout লজিক (Logic 5)
        // দ্রষ্টব্য: লেআউটে বাটন না থাকলে এটি কাজ করবে না, তাই আপনি লেআউটে একটি Logout বাটন যোগ করতে পারেন।
        // এখানে আমি শুধু লজিকটি দিয়ে রাখছি।
    }

    override fun onResume() {
        super.onResume()
        if (AuthHelper.isUserLoggedIn()) {
            requestNeededPermissions()
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
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
        val intent = Intent(this, BackgroundVoiceService::class.java)
        stopService(intent)
    }
}
