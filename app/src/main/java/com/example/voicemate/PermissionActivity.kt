package com.example.voicemate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
                checkAllPermissionsAndProceed()
            } else {
                Toast.makeText(this, "মাইক্রোফোন অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (areAllPermissionsGranted()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_permission)

        val btnMic = findViewById<LinearLayout>(R.id.btnMicPermission)
        val btnOverlay = findViewById<LinearLayout>(R.id.btnOverlayPermission)
        val btnAccessibility = findViewById<LinearLayout>(R.id.btnAccessibilityPermission)
        val btnAppPermissions = findViewById<LinearLayout>(R.id.btnAppPermissions)
        val btnGiveAll = findViewById<Button>(R.id.btnGiveAllPermissions)
        val tvNotNow = findViewById<TextView>(R.id.tvNotNow)

        btnMic.setOnClickListener {
            requestMicPermission()
        }

        btnOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        btnAccessibility.setOnClickListener {
            try {
                // অ্যাক্সেসিবিলিটি সেটিংস ওপেন হবে
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "Installed Services থেকে 'Voice Mate' অন করুন", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "সেটিংস খুঁজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            }
        }

        btnAppPermissions.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnGiveAll.setOnClickListener {
            if (!isMicPermissionGranted()) {
                requestMicPermission()
            } else if (!isOverlayPermissionGranted()) {
                requestOverlayPermission()
            } else {
                checkAllPermissionsAndProceed()
            }
        }

        tvNotNow.setOnClickListener {
            finish()
        }
    }

    private fun isMicPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        return isMicPermissionGranted() && isOverlayPermissionGranted()
    }

    private fun requestMicPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun checkAllPermissionsAndProceed() {
        if (areAllPermissionsGranted()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (areAllPermissionsGranted()) {
            checkAllPermissionsAndProceed()
        }
    }
}
