package com.example.palmdown.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.example.palmdown.MainActivity

class WelcomeActivity : ComponentActivity() {

    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { res ->
            onSignInResult(res)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se l'utente è già autenticato
        if (FirebaseAuth.getInstance().currentUser != null) {
            startMainActivity()
            return
        }

        setContent {
            MaterialTheme {
                WelcomeScreen(
                    onSignInClick = { launchSignIn() }
                )
            }
        }
    }

    private fun launchSignIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        signInLauncher.launch(signInIntent)
    }


    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            startMainActivity()
        } else {
            // Login fallito o annullato
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
private fun WelcomeScreen(
    onSignInClick: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Benvenuto",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onSignInClick) {
                Text(text = "Accedi")
            }
        }
    }
}
