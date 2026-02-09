package com.example.palmdown.ui.editor

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.palmdown.model.News
import com.example.palmdown.model.Notes
import com.example.palmdown.repository.NewsRepository
import com.example.palmdown.repository.NotesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val noteId = intent.getStringExtra("NOTE_ID") ?: UUID.randomUUID().toString()
        val initialTitle = intent.getStringExtra("NOTE_TITLE") ?: ""
        val initialContent = intent.getStringExtra("NOTE_CONTENT") ?: ""
        val noteDate = intent.getLongExtra("NOTE_DATE", System.currentTimeMillis()).takeIf { it > 0 } ?: System.currentTimeMillis()
        val initialPinned = intent.getBooleanExtra("NOTE_PINNED", false)
        val initialArchived = intent.getBooleanExtra("NOTE_ARCHIVED", false)

        setContent {
            EditorScreen(
                noteId = noteId,
                initialTitle = initialTitle,
                initialContent = initialContent,
                initialDate = noteDate,
                initialPinned = initialPinned,
                initialArchived = initialArchived,
                onFinish = { finish() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    noteId: String,
    initialTitle: String,
    initialContent: String,
    initialDate: Long,
    initialPinned: Boolean,
    initialArchived: Boolean,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notesRepo = remember { NotesRepository() }
    val newsRepo = remember { NewsRepository() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isLightMode by remember { mutableStateOf(false) }

    var isPinned by remember { mutableStateOf(initialPinned) }
    var isArchived by remember { mutableStateOf(initialArchived) }

    var showNewsPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchesCount by remember { mutableStateOf(0) }
    var findCurrentIndex by remember { mutableStateOf(0) }
    val findFocusRequester = remember { FocusRequester() }

    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }

    val bgDark = Color(0xFF121212)
    val cardDark = Color(0xFF1C1C1E)
    val accentCyan = Color(0xFF0097A7)
    val accentPurple = Color(0xFF632F96)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFF8E8E93)
    val searchBarBg = Color(0xFF2C2C2E)
    val destructiveRed = Color(0xFFFF453A)
    val warningOrange = Color(0xFFFF9F0A)

    val accentColor = if (isArchived) accentCyan else accentPurple

    val backgroundColor = if (isLightMode) Color.White else bgDark
    val headerColor = if (isLightMode) Color(0xFFF2F2F7) else bgDark
    val textColor = if (isLightMode) Color.Black else textPrimary
    val secondaryTextColor = if (isLightMode) Color.Gray else textSecondary
    val menuBgColor = if (isLightMode) Color.White else cardDark
    val menuBorderColor = if (isLightMode) Color(0xFFE0E0E0) else Color(0xFF333333)

    LaunchedEffect(noteId) {
        val note = notesRepo.getNoteById(noteId)
        if (note != null) {
            isPinned = note.pinned
            isArchived = note.archived
        }
    }

    LaunchedEffect(isLightMode, isArchived) {
        val jsBg = if (isLightMode) "#FFFFFF" else "#121212"
        val jsColor = if (isLightMode) "#000000" else "#EEEEEE"
        val jsSecColor = if (isLightMode) "#8E8E93" else "#8E8E93"

        val linkHex = if (isArchived) "#0097A7" else "#632F96"

        val chipBg = if (isLightMode) "#F0F0F5" else "#1C1C1E"
        val chipBorder = if (isLightMode) "#D1D1D6" else "#333333"
        val chipText = if (isLightMode) "#000000" else "#EEEEEE"
        val chipHover = if (isLightMode) "#E5E5EA" else "#3A3A3C"

        webViewRef?.evaluateJavascript(
            """
            document.documentElement.style.setProperty('--bg-color', '$jsBg');
            document.documentElement.style.setProperty('--text-color', '$jsColor');
            document.documentElement.style.setProperty('--sec-text-color', '$jsSecColor');
            document.documentElement.style.setProperty('--link-color', '$linkHex');
            document.documentElement.style.setProperty('--chip-bg', '$chipBg');
            document.documentElement.style.setProperty('--chip-border', '$chipBorder');
            document.documentElement.style.setProperty('--chip-text', '$chipText');
            document.documentElement.style.setProperty('--chip-hover', '$chipHover');
            """.trimIndent(), null
        )
    }

    LaunchedEffect(showFindBar) {
        if (showFindBar) {
            delay(100)
            findFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(findQuery) {
        if (findQuery.isNotEmpty()) {
            webViewRef?.findAllAsync(findQuery)
        } else {
            webViewRef?.clearMatches()
            findMatchesCount = 0
            findCurrentIndex = 0
        }
    }

    fun saveAndExit() {
        if (isSaving) return
        isSaving = true

        val script = """
            (function() {
                var title = document.getElementById('title-input').innerText;
                var content = document.getElementById('content-area').innerHTML;
                return JSON.stringify({title: title, content: content});
            })();
        """.trimIndent()

        webViewRef?.evaluateJavascript(script) { resultJson ->
            try {
                val cleanJson = resultJson.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"").replace("\\\\", "\\")
                val jsonObj = org.json.JSONObject(cleanJson)
                val extractedTitle = jsonObj.optString("title").trim()
                var extractedContent = jsonObj.optString("content").trim()

                if (extractedContent == "<br>") extractedContent = ""

                scope.launch {
                    if (extractedTitle.isBlank() && extractedContent.isBlank()) {
                        notesRepo.deleteNote(noteId)
                    } else {
                        notesRepo.saveNote(
                            Notes(
                                id = noteId,
                                title = extractedTitle,
                                content = extractedContent,
                                date = System.currentTimeMillis(),
                                pinned = isPinned,
                                archived = isArchived
                            )
                        )
                    }
                    onFinish()
                }
            } catch (e: Exception) {
                onFinish()
            }
        }
    }

    fun deleteNote() {
        scope.launch {
            notesRepo.deleteNote(noteId)
            onFinish()
        }
    }

    BackHandler { saveAndExit() }

    MaterialTheme(
        colorScheme = if (isLightMode) lightColorScheme(primary = accentColor) else darkColorScheme(primary = accentColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {

                Surface(
                    color = headerColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier.zIndex(10f)
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { saveAndExit() }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = accentColor
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Notes",
                                    color = accentColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    scope.launch {
                                        val script = """
                                            (function() {
                                                var title = document.getElementById('title-input').innerText;
                                                var content = document.getElementById('content-area').innerText;
                                                return JSON.stringify({title: title, content: content});
                                            })();
                                        """.trimIndent()

                                        webViewRef?.evaluateJavascript(script) { resultJson ->
                                            try {
                                                val cleanJson = resultJson.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"").replace("\\\\", "\\")
                                                val jsonObj = org.json.JSONObject(cleanJson)
                                                val t = jsonObj.optString("title").trim()
                                                val c = jsonObj.optString("content").trim()

                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "$t\n\n$c")
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                                            } catch (e: Exception) { }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Share", tint = accentColor)
                                }

                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreHoriz,
                                            contentDescription = "Menu",
                                            tint = accentColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        offset = DpOffset((-12).dp, 12.dp),
                                        containerColor = Color.Transparent,
                                        tonalElevation = 0.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        shadowElevation = 0.dp
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = menuBgColor,
                                            shadowElevation = 8.dp,
                                            border = BorderStroke(0.5.dp, menuBorderColor),
                                            modifier = Modifier.widthIn(min = 220.dp)
                                        ) {
                                            Column {
                                                MenuOptionItem(
                                                    text = if (isPinned) "Unpin Note" else "Pin Note",
                                                    icon = if (isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                                                    textColor = textColor,
                                                    iconColor = accentColor,
                                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                                    onClick = {
                                                        isPinned = !isPinned
                                                        showMenu = false
                                                    }
                                                )

                                                Divider(color = menuBorderColor, thickness = 0.5.dp)

                                                MenuOptionItem(
                                                    text = if (isArchived) "Unarchive" else "Archive",
                                                    icon = if (isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                                    textColor = textColor,
                                                    iconColor = accentColor,
                                                    shape = RoundedCornerShape(0.dp),
                                                    onClick = {
                                                        isArchived = !isArchived
                                                        showMenu = false
                                                    }
                                                )

                                                Divider(color = menuBorderColor, thickness = 0.5.dp)

                                                MenuOptionItem(
                                                    text = "Find in Note",
                                                    icon = Icons.Rounded.Search,
                                                    textColor = textColor,
                                                    iconColor = accentColor,
                                                    shape = RoundedCornerShape(0.dp),
                                                    onClick = {
                                                        showMenu = false
                                                        showFindBar = true
                                                    }
                                                )

                                                Divider(color = menuBorderColor, thickness = 0.5.dp)

                                                MenuOptionItem(
                                                    text = if (isLightMode) "Dark Background" else "Light Background",
                                                    icon = if (isLightMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                                    textColor = textColor,
                                                    iconColor = accentColor,
                                                    shape = RoundedCornerShape(0.dp),
                                                    onClick = {
                                                        isLightMode = !isLightMode
                                                        showMenu = false
                                                    }
                                                )

                                                Divider(color = menuBorderColor, thickness = 0.5.dp)

                                                MenuOptionItem(
                                                    text = "Delete",
                                                    icon = Icons.Rounded.Delete,
                                                    textColor = destructiveRed,
                                                    iconColor = destructiveRed,
                                                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                                    onClick = {
                                                        showMenu = false
                                                        deleteNote()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showFindBar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(headerColor)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = findQuery,
                                    onValueChange = { findQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                                    cursorBrush = SolidColor(accentColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isLightMode) Color(0xFFE5E5EA) else cardDark)
                                        .focusRequester(findFocusRequester)
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .padding(horizontal = 8.dp),
                                    decorationBox = { inner ->
                                        if (findQuery.isEmpty()) Text("Find...", color = secondaryTextColor)
                                        inner()
                                    }
                                )

                                if (findMatchesCount > 0) {
                                    Text(
                                        text = "$findCurrentIndex/$findMatchesCount",
                                        color = secondaryTextColor,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                } else if (findQuery.isNotEmpty()) {
                                    Text(
                                        text = "0/0",
                                        color = secondaryTextColor,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                IconButton(onClick = { webViewRef?.findNext(false) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor)
                                }
                                IconButton(onClick = { webViewRef?.findNext(true) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor)
                                }
                                Text(
                                    "Done",
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            showFindBar = false
                                            webViewRef?.clearMatches()
                                            findQuery = ""
                                        }
                                        .padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            ctx.startActivity(intent)
                                            return true
                                        }
                                        return false
                                    }
                                }
                                setBackgroundColor(0x00000000)
                                isVerticalScrollBarEnabled = false

                                setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                                    if (isDoneCounting) {
                                        findMatchesCount = numberOfMatches
                                        findCurrentIndex = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                                    }
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onNewsClick(url: String) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        ctx.startActivity(intent)
                                    }
                                    @JavascriptInterface
                                    fun onHaptic(type: Int) {
                                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                    @JavascriptInterface
                                    fun onStyleUpdate(bold: Boolean, italic: Boolean, underline: Boolean) {
                                        isBoldActive = bold
                                        isItalicActive = italic
                                        isUnderlineActive = underline
                                    }
                                    @JavascriptInterface
                                    fun onLinkClick(url: String) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        ctx.startActivity(intent)
                                    }
                                }, "Android")

                                val dateStr = SimpleDateFormat("d MMMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date(initialDate))

                                val htmlTemplate = """
                                    <html>
                                    <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                                    <style>
                                        :root {
                                            --bg-color: #121212;
                                            --text-color: #EEEEEE;
                                            --sec-text-color: #8E8E93;
                                            --link-color: #632F96;
                                            --chip-bg: #1C1C1E;
                                            --chip-border: #333333;
                                            --chip-text: #EEEEEE;
                                            --chip-hover: #3A3A3C;
                                        }
                                        @font-face {
                                            font-family: 'System';
                                            src: local('sans-serif');
                                        }
                                        body { 
                                            background-color: transparent; 
                                            color: var(--text-color); 
                                            font-family: 'System', sans-serif; 
                                            font-size: 18px; 
                                            line-height: 1.6; 
                                            margin: 0; 
                                            padding: 0 20px 120px 20px;
                                            -webkit-tap-highlight-color: transparent;
                                            caret-color: var(--link-color);
                                        }
                                        #meta-date {
                                            text-align: center;
                                            color: var(--link-color);
                                            opacity: 0.6;
                                            font-size: 14px;
                                            font-weight: 500;
                                            margin-top: 16px;
                                            margin-bottom: 8px;
                                        }
                                        #title-input {
                                            font-size: 34px;
                                            font-weight: bold;
                                            color: var(--text-color);
                                            margin-bottom: 16px;
                                            outline: none;
                                            display: block;
                                        }
                                        #title-input:empty:before {
                                            content: attr(placeholder);
                                            color: var(--sec-text-color);
                                            opacity: 0.6;
                                        }
                                        #content-area {
                                            outline: none;
                                            min-height: 60vh;
                                        }
                                        a { color: var(--link-color); text-decoration: none; font-weight: bold; }
                                        
                                        .news-chip {
                                            display: inline-flex;
                                            align-items: center;
                                            background-color: var(--chip-bg);
                                            border: 0.5px solid var(--chip-border);
                                            border-radius: 50px;
                                            padding: 4px 12px 4px 5px;
                                            margin: 4px 3px;
                                            vertical-align: middle;
                                            font-size: 15px;
                                            color: var(--chip-text);
                                            user-select: none;
                                            -webkit-user-select: none;
                                            cursor: default;
                                            white-space: nowrap;
                                            height: 32px;
                                            transition: all 0.2s;
                                        }
                                        .news-chip:active {
                                            background-color: var(--chip-hover);
                                            transform: scale(0.96);
                                        }
                                        .news-chip img {
                                            width: 24px;
                                            height: 24px;
                                            border-radius: 50%;
                                            margin-right: 8px;
                                            object-fit: cover;
                                            background-color: #333;
                                        }
                                        
                                        #trash-can {
                                            position: fixed;
                                            bottom: 30px;
                                            left: 50%;
                                            transform: translateX(-50%) scale(0.5);
                                            width: 60px;
                                            height: 60px;
                                            background-color: #FF453A;
                                            border-radius: 50%;
                                            display: flex;
                                            justify-content: center;
                                            align-items: center;
                                            box-shadow: 0 4px 12px rgba(0,0,0,0.5);
                                            opacity: 0;
                                            pointer-events: none;
                                            transition: opacity 0.2s, transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
                                            z-index: 100;
                                        }
                                        #trash-can.visible {
                                            opacity: 1;
                                            transform: translateX(-50%) scale(1);
                                        }
                                        #trash-can.active {
                                            transform: translateX(-50%) scale(1.2);
                                            background-color: #FF3B30;
                                        }
                                        #trash-icon {
                                            width: 24px;
                                            height: 24px;
                                            fill: white;
                                        }
                                        
                                        #ghost {
                                            position: fixed;
                                            pointer-events: none;
                                            z-index: 999;
                                            opacity: 0.9;
                                            display: none;
                                            transform: scale(1.1);
                                            box-shadow: 0 8px 16px rgba(0,0,0,0.5);
                                        }
                                        .dragging { opacity: 0.2; }
                                    </style>
                                    </head>
                                    <body>
                                        <div id="meta-date" contenteditable="false">$dateStr</div>
                                        <div id="title-input" contenteditable="true" placeholder="Title">$initialTitle</div>
                                        <div id="content-area" contenteditable="true" placeholder="Start typing...">$initialContent</div>
                                        
                                        <div id="trash-can">
                                            <svg id="trash-icon" viewBox="0 0 24 24">
                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                                            </svg>
                                        </div>
                                        
                                        <script>
                                            let dragItem = null;
                                            let ghost = null;
                                            let longPressTimer;
                                            let isDragging = false;
                                            let trashCan = document.getElementById('trash-can');
                                            
                                            function execCmd(command, value = null) {
                                                document.execCommand(command, false, value);
                                                checkStyles();
                                            }
                                            
                                            function checkStyles() {
                                                var bold = document.queryCommandState('bold');
                                                var italic = document.queryCommandState('italic');
                                                var underline = document.queryCommandState('underline');
                                                Android.onStyleUpdate(bold, italic, underline);
                                            }
                                            
                                            document.addEventListener('selectionchange', checkStyles);
                                            document.getElementById('content-area').addEventListener('click', checkStyles);
                                            document.getElementById('content-area').addEventListener('keyup', checkStyles);
                                            document.getElementById('content-area').addEventListener('input', function(e) {
                                                var selection = window.getSelection();
                                                if (selection.rangeCount > 0) {
                                                    var node = selection.anchorNode;
                                                    if (node.nodeType === 3) node = node.parentNode;
                                                    if (node.tagName === 'A') {
                                                        document.execCommand('unlink', false, null);
                                                    }
                                                }
                                            });
                                            
                                            document.addEventListener('click', function(e) {
                                                var target = e.target;
                                                while (target != null && target.tagName !== 'A') {
                                                    target = target.parentNode;
                                                }
                                                if (target && target.tagName === 'A') {
                                                    e.preventDefault();
                                                    Android.onLinkClick(target.href);
                                                }
                                            });

                                            window.onload = function() {
                                                document.querySelectorAll('.news-chip').forEach(el => delete el.dataset.init);
                                                initChips();
                                                checkStyles();
                                            };

                                            function initChips() {
                                                document.querySelectorAll('.news-chip').forEach(el => {
                                                    if(!el.dataset.init) {
                                                        el.dataset.init = true;
                                                        
                                                        el.onclick = function(e) {
                                                            if(!isDragging) {
                                                                e.stopPropagation();
                                                                e.preventDefault();
                                                                Android.onNewsClick(el.dataset.url);
                                                                return false;
                                                            }
                                                        };
                                                        
                                                        el.addEventListener('touchstart', (e) => {
                                                            longPressTimer = setTimeout(() => startDrag(e, el), 300);
                                                        }, {passive: false});
                                                        
                                                        el.addEventListener('touchmove', (e) => {
                                                            if (longPressTimer && !isDragging) {
                                                                clearTimeout(longPressTimer);
                                                                longPressTimer = null;
                                                            }
                                                            if (isDragging) {
                                                                e.preventDefault();
                                                                moveDrag(e);
                                                            }
                                                        }, {passive: false});
                                                        
                                                        el.addEventListener('touchend', (e) => {
                                                            if (longPressTimer) clearTimeout(longPressTimer);
                                                            if (isDragging) endDrag(e);
                                                        });
                                                    }
                                                });
                                            }

                                            function startDrag(e, el) {
                                                isDragging = true;
                                                dragItem = el;
                                                Android.onHaptic(0);
                                                
                                                ghost = el.cloneNode(true);
                                                ghost.id = 'ghost';
                                                document.body.appendChild(ghost);
                                                
                                                const touch = e.touches[0];
                                                updateGhostPos(touch.clientX, touch.clientY);
                                                ghost.style.display = 'inline-flex';
                                                
                                                dragItem.classList.add('dragging');
                                                trashCan.classList.add('visible');
                                                document.activeElement.blur();
                                            }

                                            function moveDrag(e) {
                                                const touch = e.touches[0];
                                                updateGhostPos(touch.clientX, touch.clientY);
                                                
                                                const trashRect = trashCan.getBoundingClientRect();
                                                const dist = Math.hypot(touch.clientX - (trashRect.left + trashRect.width/2), touch.clientY - (trashRect.top + trashRect.height/2));
                                                
                                                if (dist < 60) {
                                                    trashCan.classList.add('active');
                                                } else {
                                                    trashCan.classList.remove('active');
                                                }
                                            }

                                            function endDrag(e) {
                                                isDragging = false;
                                                
                                                const touch = e.changedTouches[0];
                                                const trashRect = trashCan.getBoundingClientRect();
                                                const dist = Math.hypot(touch.clientX - (trashRect.left + trashRect.width/2), touch.clientY - (trashRect.top + trashRect.height/2));
                                                
                                                trashCan.classList.remove('visible');
                                                trashCan.classList.remove('active');
                                                
                                                if (dist < 60) {
                                                    dragItem.remove();
                                                    Android.onHaptic(0);
                                                } else {
                                                    const range = document.caretRangeFromPoint(touch.clientX, touch.clientY);
                                                    if (range) {
                                                        range.insertNode(dragItem);
                                                        range.collapse(false);
                                                    }
                                                }
                                                
                                                dragItem.classList.remove('dragging');
                                                ghost.remove();
                                                ghost = null;
                                                dragItem = null;
                                            }

                                            function updateGhostPos(x, y) {
                                                if(ghost) {
                                                    ghost.style.left = (x - ghost.offsetWidth/2) + 'px';
                                                    ghost.style.top = (y - ghost.offsetHeight/2) + 'px';
                                                }
                                            }

                                            function insertNews(title, icon, url) {
                                                const cleanTitle = title.length > 25 ? title.substring(0, 25) + '...' : title;
                                                const html = `<span class="news-chip" contenteditable="false" data-url="`+url+`"><img src="`+icon+`" onerror="this.style.display='none'"/>` + cleanTitle + `</span>&nbsp;`;
                                                
                                                const contentArea = document.getElementById('content-area');
                                                contentArea.focus();
                                                
                                                const selection = window.getSelection();
                                                if (selection.rangeCount > 0 && contentArea.contains(selection.anchorNode)) {
                                                    document.execCommand('insertHTML', false, html);
                                                } else {
                                                    contentArea.insertAdjacentHTML('beforeend', html);
                                                }
                                                
                                                setTimeout(initChips, 50);
                                                checkStyles();
                                            }
                                            
                                            setInterval(initChips, 1000);
                                        </script>
                                    </body>
                                    </html>
                                """.trimIndent()

                                loadDataWithBaseURL(null, htmlTemplate, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webViewRef = it }
                    )
                }

                RichToolbar(
                    onBold = {
                        webViewRef?.requestFocus()
                        webViewRef?.evaluateJavascript("execCmd('bold')", null)
                    },
                    onItalic = {
                        webViewRef?.requestFocus()
                        webViewRef?.evaluateJavascript("execCmd('italic')", null)
                    },
                    onUnderline = {
                        webViewRef?.requestFocus()
                        webViewRef?.evaluateJavascript("execCmd('underline')", null)
                    },
                    onBullet = {
                        webViewRef?.requestFocus()
                        webViewRef?.evaluateJavascript("execCmd('insertUnorderedList')", null)
                    },
                    onLink = { showLinkDialog = true },
                    onNews = { showNewsPicker = true },
                    isLightMode = isLightMode,
                    accentColor = accentColor,
                    isBoldActive = isBoldActive,
                    isItalicActive = isItalicActive,
                    isUnderlineActive = isUnderlineActive
                )
            }

            AnimatedVisibility(
                visible = showNewsPicker,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.zIndex(2f)
            ) {
                NewsPickerSheet(
                    onDismiss = { showNewsPicker = false },
                    onNewsSelected = { news ->
                        val js = "insertNews('${news.title.replace("'", "\\'")}', '${news.sourceIcon}', '${news.url}')"
                        webViewRef?.evaluateJavascript(js, null)
                        showNewsPicker = false

                        scope.launch {
                            newsRepo.linkNewsToNote(noteId, news)
                        }
                    },
                    newsRepo = newsRepo,
                    accentColor = accentColor,
                    isLightMode = isLightMode,
                    bgDark = bgDark,
                    cardDark = cardDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    searchBarBg = searchBarBg
                )
            }

            if (showLinkDialog) {
                LinkInsertDialog(
                    onDismiss = { showLinkDialog = false },
                    onConfirm = { text, url ->
                        val safeText = text.ifBlank { url }
                        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                        val html = "<a href='$finalUrl'>$safeText</a>"
                        webViewRef?.requestFocus()
                        webViewRef?.evaluateJavascript("document.execCommand('insertHTML', false, \"$html\")", null)
                        showLinkDialog = false
                    },
                    accentColor = accentColor,
                    isLightMode = isLightMode,
                    bgDark = cardDark,
                    textPrimary = textPrimary
                )
            }
        }
    }
}

@Composable
fun RichToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onBullet: () -> Unit,
    onLink: () -> Unit,
    onNews: () -> Unit,
    isLightMode: Boolean,
    accentColor: Color,
    isBoldActive: Boolean = false,
    isItalicActive: Boolean = false,
    isUnderlineActive: Boolean = false
) {
    Surface(
        color = if (isLightMode) Color(0xFFF2F2F7) else Color(0xFF1C1C1E),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ToolbarBtn(Icons.Outlined.FormatBold, onBold, isLightMode, isActive = isBoldActive, activeColor = accentColor)
            ToolbarBtn(Icons.Outlined.FormatItalic, onItalic, isLightMode, isActive = isItalicActive, activeColor = accentColor)
            ToolbarBtn(Icons.Outlined.FormatUnderlined, onUnderline, isLightMode, isActive = isUnderlineActive, activeColor = accentColor)
            ToolbarBtn(Icons.Outlined.FormatListBulleted, onBullet, isLightMode)

            Box(Modifier.width(1.dp).height(20.dp).background(if (isLightMode) Color.LightGray else Color(0xFF333333)))

            ToolbarBtn(Icons.Outlined.Link, onLink, isLightMode)
            ToolbarBtn(Icons.Outlined.Newspaper, onNews, isLightMode, tint = accentColor)
        }
    }
}

@Composable
fun ToolbarBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    isLightMode: Boolean,
    tint: Color? = null,
    isActive: Boolean = false,
    activeColor: Color? = null
) {
    val finalTint = if (isActive && activeColor != null) activeColor else tint ?: if (isLightMode) Color.DarkGray else Color(0xFFB0B0B0)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive && activeColor != null) activeColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = finalTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun NewsPickerSheet(
    onDismiss: () -> Unit,
    onNewsSelected: (News) -> Unit,
    newsRepo: NewsRepository,
    accentColor: Color,
    isLightMode: Boolean,
    bgDark: Color,
    cardDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    searchBarBg: Color
) {
    var query by remember { mutableStateOf("") }
    var newsList by remember { mutableStateOf<List<News>>(emptyList()) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val sheetBg = if (isLightMode) Color(0xFFF2F2F7) else bgDark
    val itemBg = if (isLightMode) Color.White else cardDark
    val textColor = if (isLightMode) Color.Black else textPrimary
    val secondaryColor = if (isLightMode) Color.Gray else textSecondary
    val searchBg = if (isLightMode) Color(0xFFE5E5EA) else searchBarBg

    LaunchedEffect(query) {
        val filter = com.example.palmdown.model.NewsFilter(query = query)
        newsList = newsRepo.getAllNews(filter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = sheetBg,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(0.5f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { inner ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(searchBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = secondaryColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text("Search news...", color = secondaryColor)
                                inner()
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(newsList) { news ->
                        NewsPickerItem(
                            news = news,
                            isExpanded = expandedId == news.id,
                            onToggle = {
                                expandedId = if (expandedId == news.id) null else news.id
                            },
                            onInsert = { onNewsSelected(news) },
                            accentColor = accentColor,
                            itemBg = itemBg,
                            textColor = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsPickerItem(
    news: News,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onInsert: () -> Unit,
    accentColor: Color,
    itemBg: Color,
    textColor: Color
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(itemBg)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (news.sourceIcon.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(news.sourceIcon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(news.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.rotate(rotation)
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInsert() }
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                if (news.imageUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(news.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(news.content, color = Color.Gray, fontSize = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)

                if (news.country.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(formatCountrySingle(news.country), color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatCountrySingle(raw: String): String {
    if (raw.isBlank()) return ""
    val cleaned = raw.removePrefix("[").removeSuffix("]").replace("\"", "")
    val first = cleaned.split(",").firstOrNull()?.trim() ?: return ""
    val lower = first.lowercase(Locale.getDefault())
    val lowercaseWords = setOf("of", "and", "the")
    return lower.split(" ").joinToString(" ") { word ->
        if (word in lowercaseWords) word else word.replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun LinkInsertDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit, accentColor: Color, isLightMode: Boolean, bgDark: Color, textPrimary: Color) {
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val bg = if (isLightMode) Color(0xFFF2F2F7) else bgDark
    val textColor = if (isLightMode) Color.Black else textPrimary

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Insert Link", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Display Text") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF444444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF444444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Button(
                        onClick = {
                            val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                            onConfirm(text, finalUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Add Link", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuOptionItem(
    text: String,
    icon: ImageVector,
    textColor: Color,
    iconColor: Color,
    shape: Shape,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}