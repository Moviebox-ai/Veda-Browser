package com.aaryo.velocity.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

@Composable
fun WallpaperCanvas(
    theme: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (theme.uppercase()) {
                "FOREST" -> drawForestAtmosphere()
                "CYBER" -> drawCyberAtmosphere()
                "MINIMAL" -> drawMinimalAtmosphere()
                else -> drawMountainAtmosphere() // Default: Stunning Mountain landscape
            }
        }
    }
}

/**
 * Draws the signature cinematic snow-capped mountain range at twilight from the mockup.
 */
private fun DrawScope.drawMountainAtmosphere() {
    val width = size.width
    val height = size.height

    // 1. Twilight sky gradient
    val skyGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF060914),
            Color(0xFF0D1226),
            Color(0xFF1E173D),
            Color(0xFF381B4B),
            Color(0xFF5A1D4E),
            Color(0xFF281C3D)
        ),
        startY = 0f,
        endY = height * 0.75f
    )
    drawRect(brush = skyGradient, size = size)

    // 2. Stars in the upper sky
    val starRandom = Random(42)
    for (i in 0 until 60) {
        val x = starRandom.nextFloat() * width
        val y = starRandom.nextFloat() * (height * 0.45f)
        val radius = starRandom.nextFloat() * 1.5f + 0.5f
        val alpha = starRandom.nextFloat() * 0.7f + 0.2f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }

    // 3. Sunset / Twilight glow behind central mountains
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x99FFA07A),
                Color(0x66E65C00),
                Color(0x33A83279),
                Color.Transparent
            ),
            center = Offset(width * 0.5f, height * 0.38f),
            radius = width * 0.6f
        ),
        radius = width * 0.6f,
        center = Offset(width * 0.5f, height * 0.38f)
    )

    // 4. Distant jagged snow-capped peaks (Layer 1)
    val distantPeakPath = Path().apply {
        moveTo(0f, height * 0.42f)
        lineTo(width * 0.12f, height * 0.34f)
        lineTo(width * 0.22f, height * 0.38f)
        lineTo(width * 0.36f, height * 0.29f)
        lineTo(width * 0.50f, height * 0.24f) // Main Central Summit
        lineTo(width * 0.64f, height * 0.32f)
        lineTo(width * 0.78f, height * 0.27f)
        lineTo(width * 0.90f, height * 0.36f)
        lineTo(width, height * 0.32f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = distantPeakPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8EEF5), // Snow on top
                Color(0xFF8895B3),
                Color(0xFF3B3E5B),
                Color(0xFF1B1D33)
            ),
            startY = height * 0.24f,
            endY = height * 0.60f
        )
    )

    // Snow face facets on central peak
    val snowRidgePath = Path().apply {
        moveTo(width * 0.50f, height * 0.24f)
        lineTo(width * 0.44f, height * 0.35f)
        lineTo(width * 0.48f, height * 0.42f)
        lineTo(width * 0.50f, height * 0.40f)
        lineTo(width * 0.55f, height * 0.36f)
        close()
    }
    drawPath(path = snowRidgePath, color = Color.White.copy(alpha = 0.85f))

    val snowRidgeLeft = Path().apply {
        moveTo(width * 0.36f, height * 0.29f)
        lineTo(width * 0.31f, height * 0.37f)
        lineTo(width * 0.38f, height * 0.41f)
        close()
    }
    drawPath(path = snowRidgeLeft, color = Color.White.copy(alpha = 0.75f))

    val snowRidgeRight = Path().apply {
        moveTo(width * 0.78f, height * 0.27f)
        lineTo(width * 0.72f, height * 0.35f)
        lineTo(width * 0.82f, height * 0.39f)
        close()
    }
    drawPath(path = snowRidgeRight, color = Color.White.copy(alpha = 0.75f))

    // 5. Midground mountain range (Layer 2)
    val midMountainPath = Path().apply {
        moveTo(0f, height * 0.46f)
        lineTo(width * 0.18f, height * 0.38f)
        lineTo(width * 0.32f, height * 0.44f)
        lineTo(width * 0.45f, height * 0.37f)
        lineTo(width * 0.60f, height * 0.42f)
        lineTo(width * 0.75f, height * 0.35f)
        lineTo(width * 0.88f, height * 0.43f)
        lineTo(width, height * 0.40f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = midMountainPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E244D),
                Color(0xFF1B1736),
                Color(0xFF100E24)
            ),
            startY = height * 0.35f,
            endY = height * 0.70f
        )
    )

    // 6. Foreground rocky foothills & dark slopes (Layer 3)
    val foreMountainPath = Path().apply {
        moveTo(0f, height * 0.52f)
        lineTo(width * 0.25f, height * 0.48f)
        lineTo(width * 0.55f, height * 0.56f)
        lineTo(width * 0.80f, height * 0.49f)
        lineTo(width, height * 0.54f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(
        path = foreMountainPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF151728),
                Color(0xFF0A0D18),
                Color(0xFF080B12)
            ),
            startY = height * 0.48f,
            endY = height
        )
    )

    // 7. Base atmospheric gradient / fog overlay so content is perfectly legible
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x66080B11),
                Color(0xD9080B11),
                Color(0xFF080B11)
            ),
            startY = height * 0.45f,
            endY = height
        ),
        size = size
    )
}

/**
 * Draws the enchanted misty forest dawn wallpaper.
 */
private fun DrawScope.drawForestAtmosphere() {
    val width = size.width
    val height = size.height

    // Dawn green / teal gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF051717),
                Color(0xFF092928),
                Color(0xFF144D42),
                Color(0xFF286B5A),
                Color(0xFF0A1D1A)
            ),
            startY = 0f,
            endY = height * 0.7f
        ),
        size = size
    )

    // Sun rays
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66A7F3D0),
                Color(0x2210B981),
                Color.Transparent
            ),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width * 0.7f
        ),
        radius = width * 0.7f,
        center = Offset(width * 0.5f, height * 0.35f)
    )

    // Pine tree silhouettes
    for (i in 0 until 18) {
        val treeX = (i / 18f) * width * 1.1f - width * 0.05f
        val treeBaseY = height * (0.45f + (i % 3) * 0.04f)
        val treeH = height * 0.18f + (i % 4) * 20f

        val treePath = Path().apply {
            moveTo(treeX, treeBaseY - treeH)
            lineTo(treeX - 18f, treeBaseY - treeH * 0.6f)
            lineTo(treeX - 12f, treeBaseY - treeH * 0.6f)
            lineTo(treeX - 24f, treeBaseY - treeH * 0.25f)
            lineTo(treeX - 18f, treeBaseY - treeH * 0.25f)
            lineTo(treeX - 30f, treeBaseY)
            lineTo(treeX + 30f, treeBaseY)
            lineTo(treeX + 18f, treeBaseY - treeH * 0.25f)
            lineTo(treeX + 24f, treeBaseY - treeH * 0.25f)
            lineTo(treeX + 12f, treeBaseY - treeH * 0.6f)
            lineTo(treeX + 18f, treeBaseY - treeH * 0.6f)
            close()
        }
        drawPath(path = treePath, color = Color(0xFF071917).copy(alpha = 0.85f))
    }

    // Fog overlay
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x88051211),
                Color(0xFF040D0C)
            ),
            startY = height * 0.45f,
            endY = height
        ),
        size = size
    )
}

/**
 * Draws the neon cyberpunk city skyline wallpaper.
 */
private fun DrawScope.drawCyberAtmosphere() {
    val width = size.width
    val height = size.height

    // Deep neon violet to black
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070414),
                Color(0xFF140833),
                Color(0xFF280A4F),
                Color(0xFF0A051A)
            ),
            startY = 0f,
            endY = height * 0.65f
        ),
        size = size
    )

    // Skyscraper silhouettes
    val buildings = listOf(
        Triple(0.05f, 0.12f, 0.40f),
        Triple(0.20f, 0.10f, 0.48f),
        Triple(0.32f, 0.15f, 0.35f),
        Triple(0.50f, 0.14f, 0.52f),
        Triple(0.66f, 0.11f, 0.42f),
        Triple(0.80f, 0.16f, 0.46f)
    )

    for ((relX, relW, relH) in buildings) {
        val bX = width * relX
        val bW = width * relW
        val bH = height * relH
        val bY = height * 0.65f - bH

        drawRect(
            color = Color(0xFF0B051D),
            topLeft = Offset(bX, bY),
            size = Size(bW, bH)
        )
        // Neon edge light
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.7f),
            start = Offset(bX, bY),
            end = Offset(bX + bW, bY),
            strokeWidth = 2f
        )
    }

    // Grid lines on horizon
    for (i in 0..8) {
        val y = height * (0.65f + i * 0.04f)
        drawLine(
            color = Color(0xFFA855F7).copy(alpha = 0.25f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.5f
        )
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x9906030F),
                Color(0xFF06030F)
            ),
            startY = height * 0.60f,
            endY = height
        ),
        size = size
    )
}

/**
 * Draws minimal obsidian mesh gradient.
 */
private fun DrawScope.drawMinimalAtmosphere() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E283C),
                Color(0xFF0F1523),
                Color(0xFF080B11)
            ),
            center = Offset(size.width * 0.5f, size.height * 0.3f),
            radius = size.width * 0.8f
        ),
        size = size
    )
}
