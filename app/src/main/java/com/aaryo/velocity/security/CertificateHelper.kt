package com.aaryo.velocity.security

import android.net.http.SslCertificate
import java.text.SimpleDateFormat
import java.util.Locale

data class CertificateInfo(
    val domain: String,
    val issuedTo: String,
    val issuedBy: String,
    val validFrom: String,
    val validTo: String,
    val isSecure: Boolean
)

object CertificateHelper {

    fun extractInfo(certificate: SslCertificate?, url: String, isSecure: Boolean): CertificateInfo {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return if (certificate != null) {
            val to = certificate.issuedTo
            val by = certificate.issuedBy
            CertificateInfo(
                domain = to?.cName ?: url,
                issuedTo = "${to?.cName ?: "Unknown"} (${to?.oName ?: "Unknown Organization"})",
                issuedBy = "${by?.cName ?: "Unknown CA"} (${by?.oName ?: "Unknown Issuer"})",
                validFrom = certificate.validNotBeforeDate?.let { dateFormat.format(it) } ?: "N/A",
                validTo = certificate.validNotAfterDate?.let { dateFormat.format(it) } ?: "N/A",
                isSecure = isSecure
            )
        } else {
            CertificateInfo(
                domain = url,
                issuedTo = "Standard Connection",
                issuedBy = if (isSecure) "TLS/HTTPS Verified" else "None (Unencrypted HTTP)",
                validFrom = "N/A",
                validTo = "N/A",
                isSecure = isSecure
            )
        }
    }
}
