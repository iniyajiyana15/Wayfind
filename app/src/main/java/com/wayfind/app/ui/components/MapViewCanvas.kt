package com.wayfind.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import com.wayfind.app.data.model.*
import com.wayfind.app.ui.theme.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun MapViewCanvas(
    floor: Floor,
    userX: Float,
    userY: Float,
    route: Route?,
    destination: Room?,
    closedDoors: List<ClosedDoor>,
    hazardDumps: List<HazardDump>,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.0f)
                    offset += pan
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // Canvas Background
        drawRect(color = Slate900, size = Size(width, height))

        // Center building mapping coordinates (building scale 1000x800 into canvas)
        val mapScaleX = width / 1300f
        val mapScaleY = height / 1000f
        val baseScale = minOf(mapScaleX, mapScaleY) * scale

        val dx = offset.x + 20f
        val dy = offset.y + 40f

        fun mapToCanvas(x: Float, y: Float): Offset {
            return Offset(x * baseScale + dx, y * baseScale + dy)
        }

        // Draw Prototype Watermark Label
        drawText(
            textMeasurer = textMeasurer,
            text = "SIMULATED BUILDING — PROTOTYPE (FLOOR ${floor.level})",
            style = TextStyle(color = Slate600, fontSize = 12.sp),
            topLeft = Offset(20f, 20f)
        )

        // 1. Draw Rooms & Category Box Fills
        floor.rooms.forEach { room ->
            val roomNode = floor.nodes.find { it.id == room.nodeId }
            if (roomNode != null) {
                val roomPos = mapToCanvas(roomNode.x, roomNode.y)
                val roomColor = when (room.category) {
                    "Academic" -> Color(0xFF1E293B)
                    "Laboratory" -> Color(0xFF0F172A)
                    "Facility" -> Color(0xFF065F46)
                    "Service" -> Color(0xFF374151)
                    else -> Color(0xFF1E293B)
                }

                drawRect(
                    color = roomColor,
                    topLeft = Offset(roomPos.x - (60f * baseScale), roomPos.y - (40f * baseScale)),
                    size = Size(120f * baseScale, 80f * baseScale)
                )
                drawRect(
                    color = Slate700,
                    topLeft = Offset(roomPos.x - (60f * baseScale), roomPos.y - (40f * baseScale)),
                    size = Size(120f * baseScale, 80f * baseScale),
                    style = Stroke(width = 2f)
                )

                // Room Title
                drawText(
                    textMeasurer = textMeasurer,
                    text = room.name,
                    style = TextStyle(color = Slate100, fontSize = (10f * baseScale).sp),
                    topLeft = Offset(roomPos.x - (50f * baseScale), roomPos.y - (10f * baseScale))
                )
            }
        }

        // 2. Draw Corridor Walls
        floor.walls.forEach { wall ->
            val p1 = mapToCanvas(wall.x1, wall.y1)
            val p2 = mapToCanvas(wall.x2, wall.y2)
            drawLine(
                color = Slate400,
                start = p1,
                end = p2,
                strokeWidth = 6f * baseScale
            )
        }

        // 3. Draw Nodes (Elevators, Stairs, Ramps, Doors)
        floor.nodes.forEach { node ->
            val pos = mapToCanvas(node.x, node.y)
            when (node.type) {
                NodeType.ELEVATOR -> {
                    val isBlocked = floor.edges.any { (it.fromNodeId == node.id || it.toNodeId == node.id) && it.isBlocked }
                    val color = if (isBlocked) HazardRed else PrimaryBlue
                    drawCircle(color = color, radius = 18f * baseScale, center = pos)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = if (isBlocked) "🛗❌" else "🛗 ${node.label.takeLast(2)}",
                        style = TextStyle(color = Color.White, fontSize = (9f * baseScale).sp),
                        topLeft = Offset(pos.x - (16f * baseScale), pos.y - (8f * baseScale))
                    )
                }
                NodeType.RAMP -> {
                    drawCircle(color = RampGreen, radius = 16f * baseScale, center = pos)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "♿ Ramp",
                        style = TextStyle(color = Color.White, fontSize = (9f * baseScale).sp),
                        topLeft = Offset(pos.x - (16f * baseScale), pos.y - (8f * baseScale))
                    )
                }
                NodeType.STAIR -> {
                    drawCircle(color = StairBrown, radius = 16f * baseScale, center = pos)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "🪵 Stairs",
                        style = TextStyle(color = Color.White, fontSize = (9f * baseScale).sp),
                        topLeft = Offset(pos.x - (16f * baseScale), pos.y - (8f * baseScale))
                    )
                }
                else -> {}
            }
        }

        // 4. Draw Closed Doors
        closedDoors.forEach { door ->
            val n1 = floor.nodes.find { it.id == door.nodeId1 }
            val n2 = floor.nodes.find { it.id == door.nodeId2 }
            if (n1 != null && n2 != null) {
                val midX = (n1.x + n2.x) / 2f
                val midY = (n1.y + n2.y) / 2f
                val pos = mapToCanvas(midX, midY)
                val doorColor = if (door.isClosed) HazardRed else AccessibleGreen
                drawCircle(color = doorColor, radius = 12f * baseScale, center = pos)
                drawText(
                    textMeasurer = textMeasurer,
                    text = if (door.isClosed) "🚪 Locked" else "🚪 Open",
                    style = TextStyle(color = Color.White, fontSize = (8f * baseScale).sp),
                    topLeft = Offset(pos.x - (15f * baseScale), pos.y - (6f * baseScale))
                )
            }
        }

        // 5. Draw Hazard Dumps
        hazardDumps.forEach { dump ->
            if (dump.isActive) {
                val pos = mapToCanvas(dump.x, dump.y)
                drawCircle(
                    color = WarningAmber.copy(alpha = 0.4f),
                    radius = dump.radius * baseScale,
                    center = pos
                )
                drawCircle(
                    color = HazardRed,
                    radius = dump.radius * baseScale,
                    center = pos,
                    style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "⚠️ DUMP / DEBRIS",
                    style = TextStyle(color = HazardRed, fontSize = (9f * baseScale).sp),
                    topLeft = Offset(pos.x - (30f * baseScale), pos.y - (6f * baseScale))
                )
            }
        }

        // 6. Draw Calculated Route Path
        route?.let { r ->
            val path = Path()
            val floorPathNodes = r.pathNodes.filter { it.floorId == floor.id }

            if (floorPathNodes.isNotEmpty()) {
                val firstPos = mapToCanvas(floorPathNodes.first().x, floorPathNodes.first().y)
                path.moveTo(firstPos.x, firstPos.y)

                for (i in 1 until floorPathNodes.size) {
                    val nodePos = mapToCanvas(floorPathNodes[i].x, floorPathNodes[i].y)
                    path.lineTo(nodePos.x, nodePos.y)
                }

                drawPath(
                    path = path,
                    color = PrimaryBlue,
                    style = Stroke(
                        width = 8f * baseScale,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )
                )
            }
        }

        // 7. Draw Destination Marker
        destination?.let { dest ->
            val destNode = floor.nodes.find { it.id == dest.nodeId }
            if (destNode != null) {
                val pos = mapToCanvas(destNode.x, destNode.y)
                drawCircle(color = DestinationYellow, radius = 22f * baseScale, center = pos)
                drawText(
                    textMeasurer = textMeasurer,
                    text = "📍 DEST",
                    style = TextStyle(color = Color.Black, fontSize = (10f * baseScale).sp),
                    topLeft = Offset(pos.x - (18f * baseScale), pos.y - (8f * baseScale))
                )
            }
        }

        // 8. Draw Virtual User Avatar Marker
        val userPos = mapToCanvas(userX, userY)
        drawCircle(color = PrimaryBlue.copy(alpha = 0.3f), radius = 24f * baseScale, center = userPos)
        drawCircle(color = PrimaryBlue, radius = 16f * baseScale, center = userPos)
        drawCircle(color = Color.White, radius = 6f * baseScale, center = userPos)
    }
}
