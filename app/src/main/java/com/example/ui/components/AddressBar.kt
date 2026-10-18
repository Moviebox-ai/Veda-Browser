package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Tab
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InsecureWarning
import com.example.ui.theme.PrivateModePurple
import com.example.ui.theme.SecureGreen
import com.example.util.UrlResolver

@Composable
fun AddressBar(
    currentTab: Tab?,
    tabCount: Int,
    isLoading: Boolean,
    progress: Int,
    onNavigate: (String) -> Unit,
    onQueryChange: (String) -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {},
    onReload: () -> Unit,
    onStop: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSiteInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    var isFocused by remember { mutableStateOf(false) }

    val rawUrl = currentTab?.url ?: ""
    val isInternal = currentTab?.isInternal == true
    val isPrivate = currentTab?.isPrivate == true

    val displayHost = remember(rawUrl, isInternal, isPrivate) {
        if (isInternal) {
            if (isPrivate) "Private Browsing" else "Velocity Home"
        } else {
            UrlResolver.extractDisplayHost(rawUrl)
        }
    }

    var textFieldValue by remember {
        val initial = if (isInternal) "" else rawUrl
        mutableStateOf(
            TextFieldValue(
                text = initial,
                selection = TextRange(initial.length)
            )
        )
    }

    // Keep text field value in sync with URL when tab changes or navigates while not editing
    LaunchedEffect(rawUrl, isFocused) {
        if (!isFocused) {
            val updated = if (isInternal) "" else rawUrl
            textFieldValue = TextFieldValue(
                text = updated,
                selection = TextRange(updated.length)
            )
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoading) (progress / 100f).coerceIn(0.05f, 1f) else 0f,
        label = "ProgressAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Address Pill
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isFocused) {
                            focusRequester.requestFocus()
                        }
                    }
                    .testTag("address_bar_container"),
                color = if (isPrivate) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stable Security / Search Status Icon Slot (fixed size to prevent layout shifting)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isFocused && !isInternal) { onOpenSiteInfo() }
                            .padding(2.dp)
                            .testTag("security_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isFocused -> {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            currentTab?.sslError != null -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "SSL Error",
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            currentTab?.isSecure == true -> {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure Connection",
                                    tint = SecureGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            isInternal -> {
                                Icon(
                                    imageVector = if (isPrivate) Icons.Default.Security else Icons.Default.Search,
                                    contentDescription = "Search or Internal",
                                    tint = if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Not Secure",
                                    tint = InsecureWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Field Area - Always attached with stable LayoutNode hierarchy
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                onQueryChange(newValue.text)
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = if (!isFocused && displayHost.isNotEmpty() && !isInternal) FontWeight.Medium else FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val query = textFieldValue.text.trim()
                                    if (query.isNotEmpty()) {
                                        onNavigate(query)
                                    }
                                    isFocused = false
                                    onFocusChange(false)
                                    focusManager.clearFocus()
                                }
                            ),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    val nowFocused = focusState.isFocused
                                    if (nowFocused != isFocused) {
                                        isFocused = nowFocused
                                        onFocusChange(nowFocused)
                                        if (nowFocused) {
                                            val currentText = if (isInternal) "" else rawUrl
                                            textFieldValue = TextFieldValue(
                                                text = currentText,
                                                selection = TextRange(0, currentText.length)
                                            )
                                            onQueryChange(currentText)
                                        }
                                    }
                                }
                                .testTag("address_text_field"),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (!isFocused && (isInternal || textFieldValue.text.isEmpty())) {
                                        Text(
                                            text = if (isInternal) {
                                                if (isPrivate) "Search or enter address (Private)" else "Search or enter address"
                                            } else {
                                                displayHost
                                            },
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (isFocused && textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Search or enter address",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // Trailing Action Slot (Fixed 36dp width to prevent text field jumping)
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFocused) {
                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        textFieldValue = TextFieldValue("")
                                        onQueryChange("")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear input",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { if (isLoading) onStop() else onReload() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (isLoading) "Stop Loading" else "Reload Page",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Tab Switcher Button with Badge
            IconButton(
                onClick = onOpenTabs,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("tab_switcher_button")
            ) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary
                        ) {
                            Text(text = tabCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Menu Overflow Button
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("browser_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Browser Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Web Loading Progress Bar
        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (isPrivate) PrivateModePurple else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
