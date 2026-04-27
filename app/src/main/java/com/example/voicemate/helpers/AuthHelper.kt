package com.example.voicemate.helpers

import android.content.Context
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

object AuthHelper {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ১. সেশন চেক: ইউজার কি লগইন করা আছে?
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // ২. পাসওয়ার্ড সেট করা আছে কি না চেক (Firestore থেকে)
    fun checkPasswordSet(onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val isSet = document.getBoolean("passwordSet") ?: false
                onResult(isSet)
            }
            .addOnFailureListener { onResult(false) }
    }

    // ৩. নতুন পাসওয়ার্ড সেট এবং লিংক করা (First Time User)
    fun setAndLinkPassword(password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val credential = EmailAuthProvider.getCredential(email, password)
        
        user.linkWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firestore আপডেট
                    val userData = hashMapOf(
                        "googleVerified" to true,
                        "passwordSet" to true,
                        "email" to email,
                        "loginProvider" to "google+password"
                    )
                    db.collection("users").document(user.uid).set(userData)
                        .addOnSuccessListener { onComplete(true, null) }
                        .addOnFailureListener { e -> onComplete(false, e.message) }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    // ৪. পাসওয়ার্ড ভুলে গেলে রিসেট ইমেইল পাঠানো
    fun sendResetEmail(email: String, onComplete: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onComplete(true, null)
                else onComplete(false, task.exception?.message)
            }
    }

    // ৫. লগআউট করা
    fun logout(context: Context) {
        auth.signOut()
        // লোকাল সেশন ক্লিয়ার করার লজিক এখানে যোগ করতে পারেন (যেমন SharedPreferences)
        val prefs = context.getSharedPreferences("voicemate_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // ৬. জিমেইল ও পাসওয়ার্ড দিয়ে রি-অথেন্টিকেশন (Existing User)
    fun reAuthenticateWithPassword(password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, password)
        
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    // ৭. ইমেইল ও পাসওয়ার্ড দিয়ে সরাসরি লগইন
    fun signInWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception?.message)
            }
    }

    // ৮. লোকাল সেশন সেভ করা
    fun saveLocalSession(context: Context, email: String) {
        val prefs = context.getSharedPreferences("voicemate_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_email", email).apply()
    }

    // ৯. লোকাল সেশন চেক করা
    fun hasLocalSession(context: Context): Boolean {
        val prefs = context.getSharedPreferences("voicemate_prefs", Context.MODE_PRIVATE)
        return prefs.contains("user_email")
    }
}
