package com.example.palmdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.example.palmdown.ui.welcome.WelcomeActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen(
                    onLogoutClick = { logout() }
                )
            }
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
private fun MainScreen(
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ===== INIZIO REGIONE TASTO LOGOUT (TEMPORANEO) =====
        TextButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text(text = "Logout")
        }
        // ===== FINE REGIONE TASTO LOGOUT (TEMPORANEO) =====

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sei nella MainActivity 🎉",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
