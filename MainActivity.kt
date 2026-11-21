package com.kylen.libloader

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF090B12)
private val Panel = Color(0xFF131827)
private val Panel2 = Color(0xFF1A2234)
private val Blue = Color(0xFF5B8CFF)
private val Purple = Color(0xFF9B70FF)
private val Muted = Color(0xFF8F9BB2)
private val White = Color(0xFFF4F6FC)

data class SavedFile(val name: String, val uri: String, val kind: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KylenApp(this) }
    }
}

@Composable
private fun KylenApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("kylen_loader", Context.MODE_PRIVATE) }
    var games by remember { mutableStateOf(readFiles(prefs, "games")) }
    var libs by remember { mutableStateOf(readFiles(prefs, "libs")) }
    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Import your own APKs and native libraries to organise them here.") }

    fun add(uri: Uri, kind: String) {
        val name = displayName(context, uri)
        val list = if (kind == "game") games else libs
        if (list.any { it.uri == uri.toString() }) { message = "That file is already in your library."; return }
        val updated = list + SavedFile(name, uri.toString(), kind)
        if (kind == "game") { games = updated; writeFiles(prefs, "games", updated) }
        else { libs = updated; writeFiles(prefs, "libs", updated) }
        message = "$name added successfully."
    }

    val gamePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; add(it, "game") }
    }
    val libPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; add(it, "lib") }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Blue)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Brush.linearGradient(listOf(Blue, Purple)), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Text("K", color = White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("KYLEN", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("LIB LOADER", color = White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Box(Modifier.background(Color(0xFF172B27), CircleShape).padding(horizontal = 11.dp, vertical = 7.dp)) {
                    Text("LOCAL", color = Color(0xFF6DE0B0), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF202E50), Color(0xFF28203F))), RoundedCornerShape(22.dp)).padding(18.dp)) {
                Column {
                    Text("YOUR MOD LIBRARY", color = Color(0xFFB9C9FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Keep your projects\nready to customise.", color = White, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Manage APKs and native .so files in one place.", color = Color(0xFFC0C9DC), fontSize = 12.sp)
                    Spacer(Modifier.height(17.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stat("${games.size}", "GAMES")
                        Stat("${libs.size}", "LIBRARIES")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(4.dp)) {
                TabButton("Games", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                TabButton("Libraries", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            }
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (selectedTab == 0) "YOUR GAMES" else "NATIVE LIBRARIES", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(if (selectedTab == 0) "APK files you imported" else "Shared libraries (.so)", color = Muted, fontSize = 11.sp)
                }
                Button(onClick = { if (selectedTab == 0) gamePicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) else libPicker.launch(arrayOf("application/octet-stream", "*/*")) }, colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("＋  Import", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search your ${if (selectedTab == 0) "games" else "libraries"}...", color = Muted, fontSize = 12.sp) }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue, unfocusedBorderColor = Panel2, focusedTextColor = White, unfocusedTextColor = White, cursorColor = Blue))
            Spacer(Modifier.height(10.dp))
            val current = if (selectedTab == 0) games else libs
            val filtered = current.filter { it.name.contains(query, true) }
            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(Panel.copy(alpha = .55f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(if (selectedTab == 0) "▣" else "◇", color = Blue, fontSize = 34.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(if (selectedTab == 0) "No games added yet" else "No libraries added yet", color = White, fontWeight = FontWeight.Bold)
                        Text("Tap Import to add your files", color = Muted, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(filtered, key = { it.uri }) { file -> FileRow(file) {
                        val updated = current.filterNot { it.uri == file.uri }
                        if (selectedTab == 0) { games = updated; writeFiles(prefs, "games", updated) }
                        else { libs = updated; writeFiles(prefs, "libs", updated) }
                        message = "${file.name} removed."
                    } }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(message, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 7.dp))
            Text("KYLEN LIB LOADER  •  PERSONAL PROJECT TOOL", color = Color(0xFF59647A), fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 13.dp))
        }
    }
}

@Composable private fun Stat(value: String, label: String) {
    Row(Modifier.background(Color(0x33202B46), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.width(7.dp)); Text(label, color = Color(0xFFB6C2DD), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable private fun TabButton(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.background(if (active) Panel2 else Color.Transparent, RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (active) White else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun FileRow(file: SavedFile, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).border(1.dp, Color(0xFF202A3D), RoundedCornerShape(15.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(if (file.kind == "game") Color(0xFF1D3158) else Color(0xFF30254E), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(if (file.kind == "game") "APK" else ".SO", color = if (file.kind == "game") Color(0xFF8DB2FF) else Color(0xFFC2A1FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (file.kind == "game") "Android application" else "Native shared library", color = Muted, fontSize = 10.sp)
        }
        Text("REMOVE", color = Color(0xFFFF8190), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRemove).padding(7.dp))
    }
}

private fun displayName(context: Context, uri: Uri): String {
    var result = uri.lastPathSegment ?: "Imported file"
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && c.moveToFirst()) result = c.getString(index) ?: result
    }
    return result
}

private fun readFiles(prefs: android.content.SharedPreferences, key: String): List<SavedFile> {
    return runCatching {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { i -> arr.getJSONObject(i).let { SavedFile(it.getString("name"), it.getString("uri"), it.getString("kind")) } }
    }.getOrDefault(emptyList())
}

private fun writeFiles(prefs: android.content.SharedPreferences, key: String, files: List<SavedFile>) {
    val arr = JSONArray()
    files.forEach { f -> arr.put(JSONObject().put("name", f.name).put("uri", f.uri).put("kind", f.kind)) }
    prefs.edit().putString(key, arr.toString()).apply()
}
