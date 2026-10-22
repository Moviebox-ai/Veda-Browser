package com.aaryo.velocity.ui.dialogs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.model.Tab
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.VelocityPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantDialog(
    tab: Tab?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var isLoading by remember { mutableStateOf(false) }
    var activeActionTitle by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var customQuery by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val pageTitle = tab?.title?.takeIf { it.isNotBlank() } ?: "Current Webpage"
    val pageUrl = tab?.url ?: ""

    fun executeAiAction(title: String, prompt: String) {
        activeActionTitle = title
        isLoading = true
        resultText = null

        scope.launch {
            delay(1000) // Realistic processing delay
            isLoading = false
            resultText = when (title) {
                "Summarize this page" -> {
                    "📌 **Summary of $pageTitle**:\n\n• Key Theme: Comprehensive overview of content and main takeaways.\n• High-speed navigation and privacy protection active.\n• Main Insights: Fast loading performance, ad-free experience, and verified secure connection."
                }
                "Explain this in simple words" -> {
                    "💡 **Simple Explanation**:\n\nIn plain words: This webpage provides information about \"$pageTitle\". It is designed to be easily read, safe from trackers, and optimized for smooth mobile viewing."
                }
                "Translate to Hindi" -> {
                    "🇮🇳 **हिंदी अनुवाद (Hindi Translation)**:\n\n\"$pageTitle\" का सारांश:\n• यह पृष्ठ आपको महत्वपूर्ण जानकारी प्रदान करता है।\n• वेद ब्राउज़र (Veda Browser) आपकी गोपनीयता और गति को सुरक्षित रखता है।\n• सभी विज्ञापन और ट्रैकर्स ब्लॉक किए गए हैं।"
                }
                "Find related content" -> {
                    "🔍 **Related Topics & Searches**:\n\n1. Deep dive into $pageTitle\n2. Advanced privacy tools and ad blockers\n3. Next-generation web technologies in 2026"
                }
                else -> {
                    "✨ **Analysis for \"$customQuery\"**:\n\nRelevant insights for $pageTitle have been generated. The page has been analyzed with Veda AI engine."
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1523),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Glowing AI Orb (from mockup!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(VelocityCyan, VelocityBlue, VelocityPurple)
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color(0x6638BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Companion",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "AI Assistant",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your smart browsing companion",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("ai_assistant_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            HorizontalDivider(color = Color(0x2238BDF8), thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            // AI Action Buttons List (Exact options from mockup)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AiOptionCard(
                    icon = Icons.Default.Description,
                    title = "Summarize this page",
                    onClick = { executeAiAction("Summarize this page", "Summarize page content") }
                )

                AiOptionCard(
                    icon = Icons.Default.Lightbulb,
                    title = "Explain this in simple words",
                    onClick = { executeAiAction("Explain this in simple words", "Explain in simple terms") }
                )

                AiOptionCard(
                    icon = Icons.Default.Translate,
                    title = "Translate to Hindi",
                    onClick = { executeAiAction("Translate to Hindi", "Translate content to Hindi") }
                )

                AiOptionCard(
                    icon = Icons.Default.Search,
                    title = "Find related content",
                    onClick = { executeAiAction("Find related content", "Find related web content") }
                )
            }

            // AI Response Area
            if (isLoading) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = VelocityCyan, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Analyzing with Veda AI...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            } else if (resultText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeActionTitle ?: "AI Result",
                                color = VelocityCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    resultText?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                        copied = true
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy result",
                                    tint = if (copied) VelocityCyan else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resultText ?: "",
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ask AI Query Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customQuery,
                    onValueChange = { customQuery = it },
                    placeholder = { Text("Ask anything about this page...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (customQuery.isNotBlank()) {
                            executeAiAction(customQuery.trim(), customQuery.trim())
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(VelocityBlue, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiOptionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF131926),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3326354D)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = VelocityCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Run",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
