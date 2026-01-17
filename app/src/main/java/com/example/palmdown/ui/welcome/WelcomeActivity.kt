package com.example.palmdown.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.example.palmdown.MainActivity
import com.example.palmdown.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.core.content.ContextCompat

class WelcomeActivity : ComponentActivity() {

    private val TAG = "WelcomeActivity"

    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { res ->
            onSignInResult(res)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (FirebaseAuth.getInstance().currentUser != null) {
                startMainActivity()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error", e)
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
        try {
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build()
            )

            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(com.firebase.ui.auth.R.style.FirebaseUI)
                .build()

            signInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching sign in", e)
            Toast.makeText(this, "Errore durante l'avvio del login", Toast.LENGTH_LONG).show()
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Sign in successful")
            startMainActivity()
        } else {
            if (response == null) {
                Log.w(TAG, "Sign in cancelled by user")
            } else {
                Log.e(TAG, "Sign in error: ${response.error?.errorCode}")
                Toast.makeText(this, "Errore di accesso: ${response.error?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity. Check your Manifest!", e)
            Toast.makeText(this, "Impossibile avviare l'app principale", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun WelcomeScreen(
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 60.dp)
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape, // prendere il Cerchio
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                        if (drawable != null) {
                            Image(
                                painter = rememberDrawablePainter(drawable = drawable),
                                contentDescription = "Logo PalmDown",
                                contentScale = ContentScale.Crop, // Ritaglia x riempire cerchio
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape) // utilizza quello di prima
                            )
                        } else {
                            Text(text = "✍️", fontSize = 40.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PalmDown",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Le tue idee, ovunque tu sia.\nSemplice. Veloce. Pulito.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "CLICK TO START",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}