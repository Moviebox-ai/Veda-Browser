package com.aaryo.velocity.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.ui.components.VelocityLogoIcon
import com.aaryo.velocity.ui.theme.VelocityBlue
import com.aaryo.velocity.ui.theme.VelocityCyan
import com.aaryo.velocity.ui.theme.VelocityPurple

/**
 * Screen 1: Splash Screen
 * Exact replica of the reference design:
 * - Deep dark space blue background
 * - Glowing planet horizon curve at bottom with cyan/purple gradient arc
 * - Stylized 3D ribbon Velocity logo
 * - "Velocity Browser"
 * - "Fast • Secure • Smart"
 * - Glowing progress bar with animated fill
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060914))
            .clickable { onSplashFinished() }
            .testTag("splash_screen")
    ) {
        // Glowing Planetary Horizon Curve at bottom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height

            // Glow path arc
            val arcPath = Path().apply {
                moveTo(-w * 0.2f, h * 0.45f)
                cubicTo(
                    w * 0.25f, h * 0.15f,
                    w * 0.75f, h * 0.15f,
                    w * 1.2f, h * 0.45f
                )
            }

            // Diffuse atmospheric glow under horizon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x332563EB),
                        Color(0x1A8B5CF6),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.4f),
                    radius = w * 0.6f
                ),
                radius = w * 0.6f,
                center = Offset(w * 0.5f, h * 0.4f)
            )

            // Neon glowing line (wide blur stroke)
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x0000E5FF),
                        Color(0x8000E5FF),
                        Color(0xFF3B82F6),
                        Color(0x80C026D3),
                        Color(0x00C026D3)
                    )
                ),
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )

            // Sharp neon horizon beam
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x2200E5FF),
                        Color(0xFF00E5FF),
                        Color(0xFF60A5FA),
                        Color(0xFFE879F9),
                        Color(0x22E879F9)
                    )
                ),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Center Content: Logo, Title, Subtitle
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VelocityLogoIcon(size = 90.dp)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Veda Browser",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fast • Secure • Smart",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp
            )
        }

        // Progress Bar at bottom (Pill track with glowing cyan-blue fill)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp)
                .width(160.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF161E33))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF2563EB),
                                Color(0xFF3B82F6)
                            )
                        )
                    )
            )
        }
    }
}
