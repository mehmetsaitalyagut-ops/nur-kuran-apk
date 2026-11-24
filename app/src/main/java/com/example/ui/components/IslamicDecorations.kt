package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslamicAmbientBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val bgTop = if (isDark) Color(0xFF04120D) else Color(0xFFFAF6EE)
    val bgMid = if (isDark) Color(0xFF071F17) else Color(0xFFF5EFE2)
    val bgBot = if (isDark) Color(0xFF0A2A1F) else Color(0xFFEDE4D2)
    val patternColor = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.08f else 0.09f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgMid, bgBot)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right mandala corner filigree inspired by classic Islamic illumination
            drawMandalaFiligree(
                center = Offset(w * 0.94f, h * 0.04f),
                maxRadius = w * 0.34f,
                color = patternColor
            )
            // Bottom-left subtle mandala filigree
            drawMandalaFiligree(
                center = Offset(w * 0.06f, h * 0.88f),
                maxRadius = w * 0.28f,
                color = patternColor
            )
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMandalaFiligree(
    center: Offset,
    maxRadius: Float,
    color: Color
) {
    val rings = 4
    for (rIdx in 1..rings) {
        val radius = maxRadius * (rIdx.toFloat() / rings)
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = 1.2f)
        )
        val petals = 12
        for (i in 0 until petals) {
            val angleRad = Math.toRadians((i * (360.0 / petals)))
            val px = center.x + cos(angleRad).toFloat() * radius
            val py = center.y + sin(angleRad).toFloat() * radius
            drawLine(
                color = color.copy(alpha = color.alpha * 0.7f),
                start = center,
                end = Offset(px, py),
                strokeWidth = 0.8f
            )
        }
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    highlightBorder: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    val elevatedTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val borderBrush = if (highlightBorder) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        )
    }

    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(surfaceColor, elevatedTint)
                )
            )
            .border(
                BorderStroke(1.dp, borderBrush),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun RubElHizbBadge(
    numberText: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp,
    isHighlighted: Boolean = false
) {
    val strokeColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
    }
    val fillColor = if (isHighlighted) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.minDimension * 0.66f
            val left = (size.width - side) / 2f
            val top = (size.height - side) / 2f

            // Two overlapping rotated squares forming the classic 8-pointed Rub el Hizb star
            val path = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(left, top, left + side, top + side))
            }
            drawPath(path = path, color = fillColor)
            drawPath(path = path, color = strokeColor, style = Stroke(width = 2.2f))
            rotate(degrees = 45f, pivot = center) {
                drawPath(path = path, color = fillColor)
                drawPath(path = path, color = strokeColor, style = Stroke(width = 2.2f))
            }
        }
        androidx.compose.material3.Text(
            text = numberText,
            style = MaterialTheme.typography.labelMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                )
            )
    )
}
