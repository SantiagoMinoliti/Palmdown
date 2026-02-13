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
import androidx.compose.ui.unit.sp
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
import com.example.palmdown.worker.MockWorkerScheduler
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MockWorkerScheduler.scheduleDailyNews(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF632F96),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1B2E)
                )
            ) {
                MainScaffold()
            }
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()

    val appBackground = Color(0xFF121212)

    Scaffold(
        containerColor = appBackground,
        contentColor = Color.White,
        bottomBar = { ModernBottomBar(navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackground)
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

    val barGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E1B2E),
            Color(0xFF121212)
        )
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .background(barGradient),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        val items = listOf(
            Triple("notes", "Notes", Pair(Icons.Filled.Create, Icons.Outlined.Create)),
            Triple("news", "News", Pair(Icons.Filled.Article, Icons.Outlined.Article)),
            Triple("settings", "Preferences", Pair(Icons.Filled.Settings, Icons.Outlined.Settings))
        )

        items.forEach { (route, label, icons) ->
            val isSelected = currentRoute == route

            NavigationBarItem(
                selected = isSelected,
                onClick = { navController.navigate(route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF632F96),
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color(0xFF8E8E93),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color(0xFF8E8E93)
                ),
                alwaysShowLabel = true
            )
        }
    }
}