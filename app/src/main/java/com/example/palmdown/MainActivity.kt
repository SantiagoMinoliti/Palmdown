package com.example.palmdown

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
            Color(0xFF7B5CFF), // viola
            Color(0xFF9C7DFF), // lilla
            Color(0xFF5ED6E6)  // ciano
        )
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(barGradient),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        IconOnlyItem(
            selected = currentRoute == "notes",
            onClick = { navController.navigate("notes") },
            iconSelected = Icons.Filled.Create,
            iconUnselected = Icons.Outlined.Create
        )

        IconOnlyItem(
            selected = currentRoute == "news",
            onClick = { navController.navigate("news") },
            iconSelected = Icons.Filled.Article,
            iconUnselected = Icons.Outlined.Article
        )

        IconOnlyItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            iconSelected = Icons.Filled.Settings,
            iconUnselected = Icons.Outlined.Settings
        )
    }
}

@Composable
private fun RowScope.IconOnlyItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    iconUnselected: androidx.compose.ui.graphics.vector.ImageVector
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) iconSelected else iconUnselected,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent
        )
    )
}
