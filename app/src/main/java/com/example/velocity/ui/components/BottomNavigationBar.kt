package com.example.velocity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.velocity.model.Tab
import com.example.velocity.ui.theme.IncognitoPurple
import com.example.velocity.ui.theme.VelocityBlue
import com.example.velocity.ui.theme.VelocityCyan

@Composable
fun BottomNavigationBar(
    tab: Tab?,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onOpenShield: () -> Unit,
    onOpenTabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncognito = tab?.isIncognito == true

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = if (isIncognito) Color(0xFF130E26) else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onGoBack,
                enabled = tab?.canGoBack == true,
                modifier = Modifier.testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (tab?.canGoBack == true) {
                        if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            // Forward button
            IconButton(
                onClick = onGoForward,
                enabled = tab?.canGoForward == true,
                modifier = Modifier.testTag("nav_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (tab?.canGoForward == true) {
                        if (isIncognito) Color.White else MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            // Home button
            IconButton(
                onClick = onGoHome,
                modifier = Modifier.testTag("nav_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (isIncognito) IncognitoPurple else VelocityBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Shield button
            IconButton(
                onClick = onOpenShield,
                modifier = Modifier.testTag("nav_shield_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Privacy",
                    tint = if (tab?.adBlockEnabled == true) VelocityCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tabs button
            IconButton(
                onClick = onOpenTabs,
                modifier = Modifier.testTag("nav_tabs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Web,
                    contentDescription = "Tabs",
                    tint = if (isIncognito) IncognitoPurple else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
