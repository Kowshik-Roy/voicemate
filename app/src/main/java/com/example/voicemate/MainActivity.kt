package com.example.voicemate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
        setContentView(R.layout.activity_main)

        val btnStop = findViewById<Button>(R.id.btnStopService)
        val btnExit = findViewById<Button>(R.id.btnExitApp)
        val btnAccessibility = findViewById<Button>(R.id.btnOpenAccessibility)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // পারমিশন সেটআপ বাটন - এটি অ্যাক্সেসিবিলিটি এবং ওভারলে উভয় সেটিংস ওপেন করবে
        btnAccessibility.setOnClickListener {
            openPermissionSettings()
        }

        btnStop.setOnClickListener {
            stopVoiceService()
            Toast.makeText(this, "সার্ভিস বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
        }

        btnExit.setOnClickListener {
            stopVoiceService()
            finishAffinity()
        }
    }

    private fun openPermissionSettings() {
        // ১. ওভারলে পারমিশন চেক ও ওপেন
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
                startActivity(intent)
                Toast.makeText(this, "প্রথমে 'Display over other apps' অন করুন", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // ২. অ্যাক্সেসিবিলিটি সেটিংস ওপেন (টাইপিং মোডের জন্য প্রয়োজন)
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Installed Services থেকে 'Voice Mate' অন করুন", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "অ্যাক্সেসিবিলিটি সেটিংস খুঁজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        requestNeededPermissions()
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
