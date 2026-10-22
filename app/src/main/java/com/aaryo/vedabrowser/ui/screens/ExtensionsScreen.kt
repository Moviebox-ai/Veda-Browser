package com.aaryo.vedabrowser.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.extension.UserscriptParser
import com.aaryo.vedabrowser.model.Extension
import com.aaryo.vedabrowser.ui.theme.VelocityBlue
import com.aaryo.vedabrowser.ui.theme.VelocityCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    extensions: List<Extension>,
    isGlobalEnabled: Boolean,
    onToggleGlobal: (Boolean) -> Unit,
    onToggleExtension: (String, Boolean) -> Unit,
    onDeleteExtension: (String) -> Unit,
    onSaveExtension: (Extension) -> Unit,
    onInstallFromUrl: (String) -> Unit,
    onSelectExtension: (Extension) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddMenuDialog by remember { mutableStateOf(false) }
    var showPasteCodeDialog by remember { mutableStateOf(false) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var pasteCodeText by remember { mutableStateOf("") }
    var installUrlText by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    val parsed = UserscriptParser.parse(content)
                    onSaveExtension(parsed)
                    Toast.makeText(context, "Installed: ${parsed.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler {
        onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Extensions & Userscripts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("extensions_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenuDialog = true },
                containerColor = VelocityCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_extension_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Extension")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Global Opt-Out Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Extensions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isGlobalEnabled) "Userscripts & built-in enhancements are active" else "All extensions are completely disabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isGlobalEnabled,
                        onCheckedChange = onToggleGlobal,
                        colors = SwitchDefaults.colors(checkedThumbColor = VelocityCyan)
                    )
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${extensions.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "USERSCRIPTS",
                    onClick = { selectedFilter = "USERSCRIPTS" },
                    label = { Text("Userscripts (${extensions.count { !it.isBuiltIn }})") }
                )
                FilterChip(
                    selected = selectedFilter == "BUILTIN",
                    onClick = { selectedFilter = "BUILTIN" },
                    label = { Text("Built-in (${extensions.count { it.isBuiltIn }})") }
                )
            }

            val filteredList = when (selectedFilter) {
                "USERSCRIPTS" -> extensions.filter { !it.isBuiltIn }
                "BUILTIN" -> extensions.filter { it.isBuiltIn }
                else -> extensions
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No extensions installed",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { ext ->
                        ExtensionItemCard(
                            extension = ext,
                            isGlobalEnabled = isGlobalEnabled,
                            onToggle = { onToggleExtension(ext.id, it) },
                            onDelete = { onDeleteExtension(ext.id) },
                            onClick = { onSelectExtension(ext) }
                        )
                    }
                }
            }
        }
    }

    // Add Extension Options Dialog
    if (showAddMenuDialog) {
        AlertDialog(
            onDismissRequest = { showAddMenuDialog = false },
            title = { Text("Add Userscript", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMenuDialog = false
                                showPasteCodeDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = VelocityCyan)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Paste Script Code", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Input raw JavaScript source directly", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMenuDialog = false
                                filePicker.launch("*/*")
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = VelocityBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Open .user.js File", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Pick from local device storage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMenuDialog = false
                                showUrlInputDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Install from URL", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Download script via direct link", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMenuDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste Code Dialog
    if (showPasteCodeDialog) {
        AlertDialog(
            onDismissRequest = { showPasteCodeDialog = false },
            title = { Text("Paste Userscript Code", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pasteCodeText,
                        onValueChange = { pasteCodeText = it },
                        label = { Text("JavaScript code with // ==UserScript==") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteCodeText.isNotBlank()) {
                            try {
                                val parsed = UserscriptParser.parse(pasteCodeText)
                                onSaveExtension(parsed)
                                showPasteCodeDialog = false
                                pasteCodeText = ""
                                Toast.makeText(context, "Installed: ${parsed.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelocityBlue)
                ) {
                    Text("Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Install from URL Dialog
    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Install from URL", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = installUrlText,
                        onValueChange = { installUrlText = it },
                        label = { Text("Script URL (https://...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (installUrlText.startsWith("http")) {
                            onInstallFromUrl(installUrlText)
                            showUrlInputDialog = false
                            installUrlText = ""
                        } else {
                            Toast.makeText(context, "Invalid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelocityBlue)
                ) {
                    Text("Download & Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExtensionItemCard(
    extension: Extension,
    isGlobalEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (extension.isBuiltIn) VelocityCyan.copy(alpha = 0.15f) else VelocityBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (extension.isBuiltIn) Icons.Default.Extension else Icons.Default.Code,
                        contentDescription = null,
                        tint = if (extension.isBuiltIn) VelocityCyan else VelocityBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extension.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${extension.version}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (extension.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = extension.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = extension.enabled && isGlobalEnabled,
                onCheckedChange = onToggle,
                enabled = isGlobalEnabled,
                colors = SwitchDefaults.colors(checkedThumbColor = VelocityCyan)
            )

            if (!extension.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete extension",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
