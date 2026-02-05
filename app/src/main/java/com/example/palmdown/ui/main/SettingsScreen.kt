package com.example.palmdown.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onLogoutClick: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val allLanguages by viewModel.availableLanguages.collectAsState()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var keywordInput by remember { mutableStateOf("") }
    var languageSearch by remember { mutableStateOf("") }
    var languagesExpanded by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    val bgDark = Color(0xFF121212)
    val cardDark = Color(0xFF1C1C1E)
    val accentPurple = Color(0xFF632F96)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFF8E8E93)
    val dividerColor = Color(0xFF2C2C2E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                languagesExpanded = false
                focusManager.clearFocus()
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = textPrimary,
                        fontSize = 32.sp
                    )
                }
                Divider(color = dividerColor, thickness = 0.5.dp)
            }

            item {
                SectionLabel("Active Monitoring")

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    EditorialInput(
                        value = keywordInput,
                        placeholder = "Add subject...",
                        onValueChange = { keywordInput = it },
                        onAction = {
                            val text = keywordInput.trim()
                            if (text.isNotEmpty()) {
                                viewModel.addKeyword(text)
                                keywordInput = ""
                            }
                        }
                    )

                    if (settings.keywords.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            settings.keywords.forEach { keyword ->
                                EditorialChip(
                                    label = keyword,
                                    onRemove = { viewModel.removeKeyword(it) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No active keywords.",
                            color = textSecondary.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionLabel("Sources")

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    EditorialInput(
                        value = languageSearch,
                        placeholder = "Filter by language...",
                        enabled = settings.languages.size < 5,
                        onValueChange = {
                            languageSearch = it
                            languagesExpanded = true
                        },
                        onAction = {}
                    )

                    AnimatedVisibility(
                        visible = languagesExpanded && languageSearch.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardDark)
                                .border(0.5.dp, dividerColor, RoundedCornerShape(6.dp))
                        ) {
                            val filtered = allLanguages
                                .filter { it.name.contains(languageSearch, ignoreCase = true) }
                                .filterNot { settings.languages.contains(it.code) }
                                .take(4)

                            if (filtered.isEmpty()) {
                                Text(
                                    "No matches found",
                                    color = textSecondary,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp
                                )
                            } else {
                                filtered.forEach { lang ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleLanguage(lang.code)
                                                languageSearch = ""
                                                languagesExpanded = false
                                                focusManager.clearFocus()
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(lang.name, color = textPrimary, fontSize = 15.sp)
                                    }
                                    if (filtered.last() != lang) Divider(color = dividerColor)
                                }
                            }
                        }
                    }

                    if (settings.languages.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            settings.languages.forEach { code ->
                                val name = allLanguages.firstOrNull { it.code == code }?.name ?: code
                                EditorialChip(
                                    label = name,
                                    onRemove = { viewModel.toggleLanguage(code) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Divider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 24.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionLabel("Notifications")

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Digest",
                            fontSize = 17.sp,
                            color = textPrimary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium
                        )

                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                viewModel.setNotificationsEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentPurple,
                                uncheckedThumbColor = Color(0xFFAAAAAA),
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = Color(0xFF555555)
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = settings.notificationsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 20.dp)
                                .fillMaxWidth()
                                .background(cardDark, RoundedCornerShape(6.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Notifications per Day",
                                    color = textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularIconButton(
                                        icon = Icons.Default.Remove,
                                        enabled = settings.notificationsPerDay > 1,
                                        onClick = { viewModel.setNotificationsPerDay(settings.notificationsPerDay - 1) }
                                    )

                                    Text(
                                        text = "${settings.notificationsPerDay}",
                                        color = textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    CircularIconButton(
                                        icon = Icons.Default.Add,
                                        enabled = settings.notificationsPerDay < 10,
                                        onClick = { viewModel.setNotificationsPerDay(settings.notificationsPerDay + 1) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log Out",
                        color = Color(0xFFCF6679),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(letterSpacing = 0.5.sp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onLogoutClick() }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "v1.0.2 • PalmDown",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF444444),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF8E8E93),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp, top = 8.dp)
    )
}

@Composable
private fun EditorialInput(
    value: String,
    placeholder: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
    onAction: () -> Unit
) {
    val containerColor = Color(0xFF1C1C1E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF666666),
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                ),
                cursorBrush = SolidColor(Color(0xFF632F96)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (value.isNotBlank() && enabled) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF632F96))
                    .clickable { onAction() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorialChip(
    label: String,
    onRemove: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color(0xFFDDDDDD),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color(0xFF888888),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove(label) }
            )
        }
    }
}

@Composable
private fun CircularIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (enabled) Color(0xFF333333) else Color(0xFF222222)
    val iconColor = if (enabled) Color.White else Color.Gray

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}