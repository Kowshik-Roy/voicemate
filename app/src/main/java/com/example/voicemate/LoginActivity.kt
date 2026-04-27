package com.example.voicemate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.voicemate.helpers.AuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // সেশন চেক (Logic 5)
        if (AuthHelper.isUserLoggedIn() && AuthHelper.hasLocalSession(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleLogin)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)

        // Gmail + Password Login (Logic 2)
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                AuthHelper.signInWithEmail(email, pass) { success, error ->
                    if (success) {
                        AuthHelper.saveLocalSession(this, email)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Continue with Verified Gmail (Logic 1 & 3)
        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Forgot Password (Logic 4)
        tvForgot.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                AuthHelper.sendResetEmail(email) { success, error ->
                    if (success) Toast.makeText(this, "Reset link sent to Gmail", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter your Gmail first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("65090125055-gf4vp1rkcc10f1utmb5uek8uqk4rdrqd.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                AuthHelper.checkPasswordSet { isSet ->
                    if (isSet) {
                        // Existing User - Show Enter App Password (Logic 3)
                        showPasswordReAuthDialog()
                    } else {
                        // First Time User - Go to Set Password (Logic 1)
                        startActivity(Intent(this, SetPasswordActivity::class.java))
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Firebase Auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPasswordReAuthDialog() {
        val intent = Intent(this, SetPasswordActivity::class.java)
        intent.putExtra("RE_AUTH_MODE", true)
        startActivity(intent)
        finish()
    }
}
