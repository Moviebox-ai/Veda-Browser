package com.aaryo.velocity.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaryo.velocity.R

/**
 * Branded Veda Browser logo using the uploaded signature V ribbon asset
 */
@Composable
fun VelocityLogoIcon(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    VedaLogoIcon(size = size, modifier = modifier)
}

@Composable
fun VedaLogoIcon(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.veda_icon_foreground),
            contentDescription = "Veda Browser Logo",
            modifier = Modifier.size(size)
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
        VedaLogoIcon(size = iconSize)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Veda Browser",
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
