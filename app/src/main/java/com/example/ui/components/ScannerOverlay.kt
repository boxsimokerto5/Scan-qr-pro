package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ScanLaserColor

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    isAutoCopyEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val boxSize = minOf(screenWidth * 0.76f, 300.dp)

        // Draw framing overlay without offscreen layers or BlendMode.Clear to avoid GPU buffer thrashing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val boxPx = boxSize.toPx()
            val left = (canvasWidth - boxPx) / 2f
            val top = (canvasHeight - boxPx) / 2f - 40.dp.toPx()
            val cornerRadiusPx = 20.dp.toPx()
            val cornerLength = 32.dp.toPx()
            val strokeThickness = 3.5.dp.toPx()
            val darkOverlayColor = Color(0x99000000)

            // 1. Draw 4 bounding rectangles around the transparent viewfinder
            // Top rect
            drawRect(
                color = darkOverlayColor,
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, top)
            )
            // Bottom rect
            drawRect(
                color = darkOverlayColor,
                topLeft = Offset(0f, top + boxPx),
                size = Size(canvasWidth, canvasHeight - (top + boxPx))
            )
            // Left rect
            drawRect(
                color = darkOverlayColor,
                topLeft = Offset(0f, top),
                size = Size(left, boxPx)
            )
            // Right rect
            drawRect(
                color = darkOverlayColor,
                topLeft = Offset(left + boxPx, top),
                size = Size(canvasWidth - (left + boxPx), boxPx)
            )

            // 2. Draw subtle border around the viewfinder
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(left, top),
                size = Size(boxPx, boxPx),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Draw high-tech neon corners
            val cornerColor = NeonCyan
            // Top-Left corner
            drawLine(
                color = cornerColor,
                start = Offset(left, top + cornerLength),
                end = Offset(left, top + cornerRadiusPx / 2),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + cornerRadiusPx / 2, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )

            // Top-Right corner
            drawLine(
                color = cornerColor,
                start = Offset(left + boxPx - cornerLength, top),
                end = Offset(left + boxPx - cornerRadiusPx / 2, top),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + boxPx, top + cornerRadiusPx / 2),
                end = Offset(left + boxPx, top + cornerLength),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )

            // Bottom-Left corner
            drawLine(
                color = cornerColor,
                start = Offset(left, top + boxPx - cornerLength),
                end = Offset(left, top + boxPx - cornerRadiusPx / 2),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + cornerRadiusPx / 2, top + boxPx),
                end = Offset(left + cornerLength, top + boxPx),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )

            // Bottom-Right corner
            drawLine(
                color = cornerColor,
                start = Offset(left + boxPx - cornerLength, top + boxPx),
                end = Offset(left + boxPx - cornerRadiusPx / 2, top + boxPx),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left + boxPx, top + boxPx - cornerRadiusPx / 2),
                end = Offset(left + boxPx, top + boxPx - cornerLength),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Round
            )

            // 4. Dynamic animated scanning laser line
            val laserY = top + (boxPx * laserPosition)
            val laserBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    ScanLaserColor.copy(alpha = 0.8f),
                    Color.White,
                    ScanLaserColor.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                startX = left,
                endX = left + boxPx
            )

            drawLine(
                brush = laserBrush,
                start = Offset(left + 8.dp.toPx(), laserY),
                end = Offset(left + boxPx - 8.dp.toPx(), laserY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Instruction Text below viewfinder
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (boxSize / 2) + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC111827), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Arahkan kamera ke Barcode atau QR Code",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            if (isAutoCopyEnabled) {
                Text(
                    text = "⚡ Salin otomatis ke clipboard aktif",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
