package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация Firestore
        firestore = FirebaseFirestore.getInstance()

        // Проверяем текущего пользователя
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Если пользователь авторизован, переходим к NotesActivity
            navigateToNotesActivity()
        } else {
            // Если пользователь не авторизован, переходим к LoginActivity
            navigateToLoginActivity()
        }
    }

    private fun navigateToNotesActivity() {
        val intent = Intent(this, NotesActivity::class.java)
        startActivity(intent)
        finish() // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
    }
}