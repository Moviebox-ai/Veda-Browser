package com.aaryo.vedabrowser.ui.dialogs

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

sealed class WebPermissionState {
    data class ResourcePermission(
        val origin: String,
        val resources: Array<String>,
        val request: PermissionRequest
    ) : WebPermissionState()

    data class GeolocationPermission(
        val origin: String,
        val callback: GeolocationPermissions.Callback
    ) : WebPermissionState()
}

@Composable
fun WebPermissionDialog(
    permission: WebPermissionState,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val origin = when (permission) {
        is WebPermissionState.ResourcePermission -> permission.origin
        is WebPermissionState.GeolocationPermission -> permission.origin
    }

    val description = when (permission) {
        is WebPermissionState.ResourcePermission -> {
            val list = permission.resources.map { res ->
                when (res) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "Microphone"
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "Camera"
                    PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "Protected Media"
                    else -> res
                }
            }
            list.joinToString(", ")
        }
        is WebPermissionState.GeolocationPermission -> "Location"
    }

    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Permission Request",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Text(
                text = "$origin wants to access your $description.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Block")
            }
        }
    )
}
