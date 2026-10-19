package com.aaryo.velocity.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.ui.theme.IncognitoPurple
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.VelocityPurple

@Composable
fun BottomNavigationBar(
    tab: Tab?,
    tabCount: Int,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncognito = tab?.isIncognito == true
    val isNewTab = tab?.isNewTab == true

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = if (isIncognito) Color(0xFF090614) else Color(0xFF0A0E17),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isIncognito) Color(0x33A855F7) else Color(0x2238BDF8)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home Button
            NavBarButton(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = isNewTab,
                isIncognito = isIncognito,
                onClick = onGoHome,
                modifier = Modifier.testTag("nav_home_button")
            )

            // If active on a real web page, show back/forward controls or tabs
            if (!isNewTab) {
                // Back Button
                IconButton(
                    onClick = onGoBack,
                    enabled = tab?.canGoBack == true,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (tab?.canGoBack == true) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Forward Button
                IconButton(
                    onClick = onGoForward,
                    enabled = tab?.canGoForward == true,
                    modifier = Modifier.testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (tab?.canGoForward == true) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Tabs Button (with badge count matching mockup!)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenTabs() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("nav_tabs_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isIncognito) IncognitoPurple else Color.White
                        ),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tabCount.toString(),
                                color = if (isIncognito) IncognitoPurple else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = "Tabs",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 3. AI Assistant Button (Glowing Orb from mockup!)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenAiAssistant() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("nav_ai_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(VelocityCyan, VelocityBlue, VelocityPurple)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = "AI",
                    color = VelocityCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 4. Menu Button (Hamburger ☰ matching mockup!)
            NavBarButton(
                icon = Icons.Default.Menu,
                label = "Menu",
                isActive = false,
                isIncognito = isIncognito,
                onClick = onOpenMenu,
                modifier = Modifier.testTag("nav_menu_button")
            )
        }
    }
}

@Composable
private fun NavBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    isIncognito: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                isActive && isIncognito -> IncognitoPurple
                isActive -> VelocityCyan
                else -> Color(0xFF94A3B8)
            },
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = when {
                isActive && isIncognito -> IncognitoPurple
                isActive -> VelocityCyan
                else -> Color(0xFF94A3B8)
            },
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
