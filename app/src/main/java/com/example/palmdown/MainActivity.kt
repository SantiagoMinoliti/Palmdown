package com.example.palmdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.palmdown.ui.main.NewsScreen
import com.example.palmdown.ui.main.SettingsScreen
import com.example.palmdown.ui.notes.NotesScreen
import com.example.palmdown.ui.welcome.WelcomeActivity
import com.example.palmdown.worker.NewsWorkerScheduler
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NewsWorkerScheduler.scheduleDailyNews(this)

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

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFAFAFA)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { ModernBottomBar(navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = "news"
            ) {
                composable("notes") { NotesScreen() }
                composable("news") { NewsScreen() }
                composable("settings") {
                    SettingsScreen(
                        onLogoutClick = {
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(navController.context, WelcomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            navController.context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernBottomBar(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val barGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF632F96), // viola
            Color(0xFF373999), // lilla
        )
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(barGradient)
            .padding(top = 12.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "notes",
            onClick = { navController.navigate("notes") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "notes") Icons.Filled.Create else Icons.Outlined.Create,
                    contentDescription = "Notes",
                    tint = if (currentRoute == "notes") Color.White else Color.White.copy(alpha = 0.6f)
                )
            },
            label = { Text("Notes") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent,
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            )
        )

        NavigationBarItem(
            selected = currentRoute == "news",
            onClick = { navController.navigate("news") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "news") Icons.Filled.Article else Icons.Outlined.Article,
                    contentDescription = "News",
                    tint = if (currentRoute == "news") Color.White else Color.White.copy(alpha = 0.6f)
                )
            },
            label = { Text("News") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent,
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            )
        )

        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Preferences",
                    tint = if (currentRoute == "settings") Color.White else Color.White.copy(alpha = 0.6f)
                )
            },
            label = { Text("Preferences") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent,
                selectedIconColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                selectedTextColor = Color.White,
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}
