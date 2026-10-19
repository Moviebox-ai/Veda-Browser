package com.aaryo.velocity.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.VelocityPurple

@Composable
fun VelocityLogoIcon(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Left wing of the V
        val leftWing = Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.48f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.85f)
            lineTo(w * 0.05f, h * 0.22f)
            close()
        }

        // Right wing of the V
        val rightWing = Path().apply {
            moveTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.52f, h * 0.85f)
            lineTo(w * 0.65f, h * 0.85f)
            lineTo(w * 0.95f, h * 0.22f)
            close()
        }

        // Center chevron glow
        val centerV = Path().apply {
            moveTo(w * 0.22f, h * 0.20f)
            lineTo(w * 0.50f, h * 0.75f)
            lineTo(w * 0.78f, h * 0.20f)
            lineTo(w * 0.65f, h * 0.20f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.35f, h * 0.20f)
            close()
        }

        val wingGradient = Brush.linearGradient(
            colors = listOf(VelocityCyan, VelocityBlue, VelocityPurple),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        drawPath(path = leftWing, brush = wingGradient)
        drawPath(path = rightWing, brush = wingGradient)
        drawPath(
            path = centerV,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), VelocityCyan),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h)
            )
        )
    }
}

@Composable
fun VelocityBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    titleSize: Int = 32,
    showTagline: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VelocityLogoIcon(size = iconSize)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Velocity",
            color = Color.White,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
