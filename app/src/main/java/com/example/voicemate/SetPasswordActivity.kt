package com.example.voicemate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voicemate.helpers.AuthHelper
import com.google.firebase.auth.FirebaseAuth

class SetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_password)

        val isReAuthMode = intent.getBooleanExtra("RE_AUTH_MODE", false)
        val etNewPass = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPass = findViewById<EditText>(R.id.etConfirmPassword)
        val btnAction = findViewById<Button>(R.id.btnSetPassword)

        if (isReAuthMode) {
            findViewById<TextView>(android.R.id.title)?.text = "Enter App Password"
            etNewPass.hint = "Enter App Password"
            etConfirmPass.visibility = android.view.View.GONE
            btnAction.text = "Unlock App"
        }

        btnAction.setOnClickListener {
            val pass1 = etNewPass.text.toString().trim()
            val pass2 = etConfirmPass.text.toString().trim()

            if (isReAuthMode) {
                // Logic 3: Re-authenticate Existing User
                AuthHelper.reAuthenticateWithPassword(pass1) { success, error ->
                    if (success) {
                        AuthHelper.saveLocalSession(this, FirebaseAuth.getInstance().currentUser?.email ?: "")
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Wrong Password!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Logic 1: Set First Time Password & Link (Steps 7-12)
                if (pass1.length >= 6 && pass1 == pass2) {
                    AuthHelper.setAndLinkPassword(pass1) { success, error ->
                        if (success) {
                            AuthHelper.saveLocalSession(this, FirebaseAuth.getInstance().currentUser?.email ?: "")
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Linking failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords must match and be at least 6 chars", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
