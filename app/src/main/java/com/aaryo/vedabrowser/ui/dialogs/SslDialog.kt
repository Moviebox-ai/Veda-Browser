package com.aaryo.vedabrowser.ui.dialogs

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.vedabrowser.security.CertificateInfo
import com.aaryo.vedabrowser.ui.theme.GreenSecure

@Composable
fun SslDialog(
    info: CertificateInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = if (info.isSecure) GreenSecure.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (info.isSecure) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (info.isSecure) GreenSecure else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (info.isSecure) "Connection is Secure" else "Connection Not Secure",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                Text(
                    text = info.domain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                CertDetailRow("Issued To", info.issuedTo)
                CertDetailRow("Issued By", info.issuedBy)
                CertDetailRow("Valid Period", "${info.validFrom} – ${info.validTo}")
                CertDetailRow("Encryption", if (info.isSecure) "TLS 1.2 / 1.3 Verified" else "None (Plaintext)")

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (info.isSecure) {
                        "Your information (passwords, cookies, messages) is private when communicated with this origin."
                    } else {
                        "Attackers might be able to intercept or alter information you send or receive from this website."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun CertDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
