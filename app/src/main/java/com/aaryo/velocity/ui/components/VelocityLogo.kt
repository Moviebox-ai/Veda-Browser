package com.aaryo.velocity.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom 3D Ribbon "V" logo matching the Velocity Browser mockup:
 * Left wing: Cyan (#00E5FF) to Electric Blue (#2563EB)
 * Right wing: Electric Blue (#3B82F6) to Vivid Purple/Magenta (#A855F7 / #D946EF)
 */
@Composable
fun VelocityLogoIcon(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Left wing ribbon
        val leftPath = Path().apply {
            moveTo(w * 0.12f, h * 0.18f)
            cubicTo(w * 0.18f, h * 0.10f, w * 0.32f, h * 0.12f, w * 0.38f, h * 0.24f)
            lineTo(w * 0.52f, h * 0.76f)
            lineTo(w * 0.40f, h * 0.88f)
            cubicTo(w * 0.32f, h * 0.94f, w * 0.22f, h * 0.90f, w * 0.18f, h * 0.80f)
            close()
        }

        // Right wing ribbon (folds over the center)
        val rightPath = Path().apply {
            moveTo(w * 0.88f, h * 0.18f)
            cubicTo(w * 0.82f, h * 0.10f, w * 0.68f, h * 0.12f, w * 0.62f, h * 0.24f)
            lineTo(w * 0.40f, h * 0.78f)
            lineTo(w * 0.50f, h * 0.90f)
            cubicTo(w * 0.58f, h * 0.95f, w * 0.70f, h * 0.90f, w * 0.75f, h * 0.80f)
            close()
        }

        val leftGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00E5FF),
                Color(0xFF0284C7),
                Color(0xFF2563EB)
            ),
            start = Offset(w * 0.1f, h * 0.1f),
            end = Offset(w * 0.55f, h * 0.9f)
        )

        val rightGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6),
                Color(0xFF8B5CF6),
                Color(0xFFD946EF)
            ),
            start = Offset(w * 0.45f, h * 0.8f),
            end = Offset(w * 0.9f, h * 0.15f)
        )

        // Draw left wing
        drawPath(path = leftPath, brush = leftGradient)

        // Draw right wing with slight overlay highlight
        drawPath(path = rightPath, brush = rightGradient)

        // Specular highlight along the fold
        val highlightPath = Path().apply {
            moveTo(w * 0.42f, h * 0.70f)
            lineTo(w * 0.48f, h * 0.82f)
        }
        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.35f),
            style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun VelocityBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 52.dp,
    titleSize: Int = 22,
    subtitle: String = "Fast • Secure • Private"
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VelocityLogoIcon(size = iconSize)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Velocity Browser",
            color = Color.White,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
