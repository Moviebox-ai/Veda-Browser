package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Tab
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InsecureWarning
import com.example.ui.theme.SecureGreen
import com.example.util.UrlResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInfoSheet(
    tab: Tab?,
    onDismiss: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onClearSiteData: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val url = tab?.url ?: ""
    val host = UrlResolver.extractDisplayHost(url)
    val isHttps = tab?.isSecure == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("site_info_sheet")
        ) {
            // Header: Host and connection indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                tab?.sslError != null -> DangerRed.copy(alpha = 0.15f)
                                isHttps -> SecureGreen.copy(alpha = 0.15f)
                                else -> InsecureWarning.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            tab?.sslError != null -> Icons.Default.Warning
                            isHttps -> Icons.Default.Lock
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when {
                            tab?.sslError != null -> DangerRed
                            isHttps -> SecureGreen
                            else -> InsecureWarning
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = host.ifEmpty { "About Velocity" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when {
                            tab?.sslError != null -> "Certificate Error"
                            isHttps -> "Secure HTTPS connection"
                            tab?.isInternal == true -> "Internal Application View"
                            else -> "Not secure (HTTP)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            tab?.sslError != null -> DangerRed
                            isHttps -> SecureGreen
                            else -> InsecureWarning
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Details
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when {
                            tab?.sslError != null -> "SSL Warning: ${tab.sslError}"
                            isHttps -> "Your connection is encrypted with modern TLS. Passwords and personal information remain private."
                            tab?.isInternal == true -> "Built-in Velocity page. Runs securely on-device with zero network footprint."
                            else -> "You should not enter passwords, payment cards, or sensitive data on this connection."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tracker Blocker Protection
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Velocity Tracker Protection",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${tab?.blockedRequestsCount ?: 0} ads & trackers blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Desktop Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Desktop site",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = tab?.desktopMode == true,
                    onCheckedChange = { onToggleDesktopMode() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clear Site Data Button
            Button(
                onClick = {
                    onClearSiteData()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear site cookies & cache")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
