package com.wayfind.app.domain

import com.wayfind.app.data.model.ClosedDoor
import com.wayfind.app.data.model.HazardDump
import com.wayfind.app.data.model.Wall
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class CollisionSystem {

    private val userAvatarRadius = 15f

    fun resolvePosition(
        currentX: Float,
        currentY: Float,
        targetX: Float,
        targetY: Float,
        walls: List<Wall>,
        closedDoors: List<ClosedDoor>,
        hazardDumps: List<HazardDump>
    ): Pair<Float, Float> {

        // Check hazard dumps collision
        hazardDumps.forEach { dump ->
            if (dump.isActive) {
                val dx = targetX - dump.x
                val dy = targetY - dump.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < (dump.radius + userAvatarRadius)) {
                    // Blocked by hazard dump, push user back
                    return Pair(currentX, currentY)
                }
            }
        }

        // Check walls collision
        for (wall in walls) {
            if (isCircleIntersectingSegment(targetX, targetY, userAvatarRadius, wall.x1, wall.y1, wall.x2, wall.y2)) {
                // Try sliding along X or Y axis
                val slideX = resolveAxisSlide(currentX, currentY, targetX, currentY, wall)
                if (slideX != null) return slideX

                val slideY = resolveAxisSlide(currentX, currentY, currentX, targetY, wall)
                if (slideY != null) return slideY

                return Pair(currentX, currentY)
            }
        }

        return Pair(targetX, targetY)
    }

    private fun resolveAxisSlide(
        origX: Float,
        origY: Float,
        testX: Float,
        testY: Float,
        wall: Wall
    ): Pair<Float, Float>? {
        if (!isCircleIntersectingSegment(testX, testY, userAvatarRadius, wall.x1, wall.y1, wall.x2, wall.y2)) {
            return Pair(testX, testY)
        }
        return null
    }

    private fun isCircleIntersectingSegment(
        cx: Float, cy: Float, r: Float,
        x1: Float, y1: Float, x2: Float, y2: Float
    ): Boolean {
        val l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
        if (l2 == 0f) return distSq(cx, cy, x1, y1) < r * r

        var t = ((cx - x1) * (x2 - x1) + (cy - y1) * (y2 - y1)) / l2
        t = max(0f, min(1f, t))

        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)

        return distSq(cx, cy, projX, projY) < r * r
    }

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)
    }
}
