package com.aaryo.vedabrowser.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.extension.UserscriptParser
import com.aaryo.vedabrowser.model.Extension
import com.aaryo.vedabrowser.ui.theme.VelocityBlue
import com.aaryo.vedabrowser.ui.theme.VelocityCyan

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtensionDetailScreen(
    extension: Extension,
    currentSiteHost: String?,
    onSaveExtension: (Extension) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSource by remember { mutableStateOf(extension.source) }
    var isEnabled by remember { mutableStateOf(extension.enabled) }
    var allowIncognito by remember { mutableStateOf(extension.allowInIncognito) }

    val cleanHost = currentSiteHost?.lowercase()?.removePrefix("www.")
    var runOnThisSite by remember {
        mutableStateOf(
            cleanHost == null || extension.siteAllowlist.isEmpty() || extension.siteAllowlist.contains(cleanHost)
        )
    }

    BackHandler {
        onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = extension.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                val reParsed = UserscriptParser.parse(currentSource).copy(
                                    id = extension.id,
                                    enabled = isEnabled,
                                    allowInIncognito = allowIncognito,
                                    isBuiltIn = extension.isBuiltIn,
                                    siteAllowlist = if (cleanHost != null && !runOnThisSite) {
                                        extension.siteAllowlist.filterNot { it == cleanHost }
                                    } else {
                                        extension.siteAllowlist
                                    }
                                )
                                onSaveExtension(reParsed)
                                Toast.makeText(context, "Saved changes", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = VelocityCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = extension.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Version ${extension.version} • By ${extension.author}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (extension.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = extension.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Switches
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enabled", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Active on matching websites", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VelocityCyan)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow in Incognito", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Execute script in private browsing tabs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowIncognito,
                            onCheckedChange = { allowIncognito = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VelocityCyan)
                        )
                    }

                    if (cleanHost != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Run on $cleanHost", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Toggle execution specifically on this origin", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = runOnThisSite,
                                onCheckedChange = { runOnThisSite = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = VelocityCyan)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Match Patterns
            Text(
                text = "Target URL Matches",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                extension.matchPatterns.forEach { pattern ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(pattern, fontSize = 11.sp) }
                    )
                }
            }

            // Permissions / Grants
            if (extension.grants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Granted GM_* APIs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    extension.grants.forEach { grant ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(grant, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Source Code Editor
            Text(
                text = "Source Code",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = currentSource,
                onValueChange = { currentSource = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    try {
                        val reParsed = UserscriptParser.parse(currentSource).copy(
                            id = extension.id,
                            enabled = isEnabled,
                            allowInIncognito = allowIncognito,
                            isBuiltIn = extension.isBuiltIn
                        )
                        onSaveExtension(reParsed)
                        Toast.makeText(context, "Saved changes", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VelocityBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes")
            }
        }
    }
}
