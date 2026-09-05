package com.wayfind.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayfind.app.ui.theme.AccentCyan
import com.wayfind.app.ui.theme.Slate800
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
    thumbSizeDp: Dp = 50.dp,
    onMove: (dx: Float, dy: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .pointerInput(Unit) {
                val radius = (sizeDp.toPx() - thumbSizeDp.toPx()) / 2f
                val deadZone = radius * 0.15f

                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)

                        if (dist > radius) {
                            val angle = atan2(newOffset.y, newOffset.x)
                            thumbOffset = Offset(cos(angle) * radius, sin(angle) * radius)
                        } else {
                            thumbOffset = newOffset
                        }

                        if (dist < deadZone) {
                            onMove(0f, 0f)
                        } else {
                            val normDx = thumbOffset.x / radius
                            val normDy = thumbOffset.y / radius
                            onMove(normDx, normDy)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val thumbRadius = thumbSizeDp.toPx() / 2f

            // Base Ring
            drawCircle(color = Slate800.copy(alpha = 0.85f), radius = outerRadius, center = center)
            drawCircle(color = AccentCyan.copy(alpha = 0.4f), radius = outerRadius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

            // Thumb Knob
            val thumbCenter = center + thumbOffset
            drawCircle(color = AccentCyan, radius = thumbRadius, center = thumbCenter)
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = thumbRadius * 0.4f, center = thumbCenter)
        }
    }
}
