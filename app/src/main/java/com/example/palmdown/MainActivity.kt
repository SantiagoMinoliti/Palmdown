package com.example.palmdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.palmdown.ui.notes.NotesScreen
import com.example.palmdown.ui.welcome.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.palmdown.ui.main.NewsScreen
import com.example.palmdown.ui.main.SettingsScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScaffold()
            }
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "notes",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("notes") {
                NotesScreen()
            }
            composable("news") {
                NewsScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(
                            navController.context,
                            WelcomeActivity::class.java
                        )
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        navController.context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(navController: androidx.navigation.NavHostController) {
    val currentRoute = navController
        .currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "notes",
            onClick = { navController.navigate("notes") },
            label = { Text("Notes") },
            icon = {}
        )
        NavigationBarItem(
            selected = currentRoute == "news",
            onClick = { navController.navigate("news") },
            label = { Text("News") },
            icon = {}
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            label = { Text("Settings") },
            icon = {}
        )
    }
}