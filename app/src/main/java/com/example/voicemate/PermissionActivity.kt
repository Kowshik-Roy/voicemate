package com.example.voicemate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                checkAllPermissionsAndProceed()
            } else {
                Toast.makeText(this, "সবগুলো পারমিশন এলাও করা প্রয়োজন", Toast.LENGTH_SHORT).show()
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

        val btnGiveAll = findViewById<Button>(R.id.btnGiveAllPermissions)
        val btnAccessibility = findViewById<LinearLayout>(R.id.btnAccessibilityPermission)
        val tvNotNow = findViewById<TextView>(R.id.tvNotNow)

        btnGiveAll.setOnClickListener {
            if (!hasRuntimePermissions()) {
                permissionLauncher.launch(requiredPermissions)
            } else if (!isOverlayPermissionGranted()) {
                requestOverlayPermission()
            } else {
                checkAllPermissionsAndProceed()
            }
        }

        btnAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, "Installed Services থেকে 'Voice Mate' অন করুন", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "সেটিংস খুঁজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            }
        }

        tvNotNow.setOnClickListener {
            finish()
        }
    }

    private fun hasRuntimePermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        return hasRuntimePermissions() && isOverlayPermissionGranted()
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
