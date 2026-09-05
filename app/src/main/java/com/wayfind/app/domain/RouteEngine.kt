package com.wayfind.app.domain

import com.wayfind.app.data.model.*
import java.util.PriorityQueue
import kotlin.math.sqrt

class RouteEngine {

    fun calculateRoute(
        startNode: Node,
        destNode: Node,
        profile: AccessibilityProfileType,
        building: Building,
        closedDoors: List<ClosedDoor>,
        hazardDumps: List<HazardDump>,
        userHeading: Float? = null
    ): Route? {
        val allNodes = mutableListOf<Node>()
        val allEdges = mutableListOf<Edge>()

        building.floors.forEach { floor ->
            allNodes.addAll(floor.nodes)
            allEdges.addAll(floor.edges)
        }

        // Add virtual inter-floor connection edges for Elevators, Stairs, Ramps
        val interFloorEdges = createInterFloorEdges()
        allEdges.addAll(interFloorEdges)

        val nodeMap = allNodes.associateBy { it.id }

        val distances = mutableMapOf<String, Float>()
        val previous = mutableMapOf<String, String>()
        val pq = PriorityQueue<Pair<String, Float>>(compareBy { it.second })

        allNodes.forEach { node ->
            distances[node.id] = Float.MAX_VALUE
        }

        distances[startNode.id] = 0f
        pq.add(Pair(startNode.id, 0f))

        while (pq.isNotEmpty()) {
            val (currentId, currentDist) = pq.poll() ?: break

            if (currentId == destNode.id) break
            if (currentDist > (distances[currentId] ?: Float.MAX_VALUE)) continue

            val currentNode = nodeMap[currentId] ?: continue

            // Find all outgoing edges
            val neighbors = allEdges.filter { it.fromNodeId == currentId || it.toNodeId == currentId }

            for (edge in neighbors) {
                val neighborId = if (edge.fromNodeId == currentId) edge.toNodeId else edge.fromNodeId
                val neighborNode = nodeMap[neighborId] ?: continue

                if (!isEdgeUsable(edge, currentNode, neighborNode, profile, closedDoors, hazardDumps)) {
                    continue
                }

                val weight = calculateEdgeWeight(edge, currentNode, neighborNode, profile, hazardDumps)
                val newDist = currentDist + weight

                if (newDist < (distances[neighborId] ?: Float.MAX_VALUE)) {
                    distances[neighborId] = newDist
                    previous[neighborId] = currentId
                    pq.add(Pair(neighborId, newDist))
                }
            }
        }

        if (distances[destNode.id] == Float.MAX_VALUE) {
            return null // No path found
        }

        // Reconstruct path
        val pathNodeIds = mutableListOf<String>()
        var curr: String? = destNode.id
        while (curr != null) {
            pathNodeIds.add(0, curr)
            curr = previous[curr]
        }

        val pathNodes = pathNodeIds.mapNotNull { nodeMap[it] }
        val steps = generateNavigationSteps(pathNodes, profile, closedDoors, hazardDumps, userHeading)
        val totalDistance = calculateTotalDistance(pathNodes)
        val estTimeMin = (totalDistance / 1.2f / 60f).toInt().coerceAtLeast(1)

        return Route(
            pathNodes = pathNodes,
            totalDistanceMeters = totalDistance,
            estimatedTimeMinutes = estTimeMin,
            steps = steps,
            isAccessibleForProfile = true,
            profileType = profile
        )
    }

    private fun isEdgeUsable(
        edge: Edge,
        fromNode: Node,
        toNode: Node,
        profile: AccessibilityProfileType,
        closedDoors: List<ClosedDoor>,
        hazardDumps: List<HazardDump>
    ): Boolean {
        if (edge.isBlocked) return false

        // Check closed doors
        val matchingDoor = closedDoors.find {
            (it.nodeId1 == fromNode.id && it.nodeId2 == toNode.id) ||
                    (it.nodeId1 == toNode.id && it.nodeId2 == fromNode.id)
        }
        if (matchingDoor != null && matchingDoor.isClosed) {
            return false
        }

        // Check wheelchair profile restrictions
        if (profile == AccessibilityProfileType.WHEELCHAIR || profile == AccessibilityProfileType.TEMPORARY_MOBILITY) {
            if (edge.hasStairs || !edge.isAccessible) return false
            if (edge.isNarrow && profile == AccessibilityProfileType.WHEELCHAIR) return false
        }

        // Check active hazard dumps blocking path
        val activeDump = hazardDumps.find { it.floorId == fromNode.floorId && it.isActive }
        if (activeDump != null) {
            val distFrom = sqrt((fromNode.x - activeDump.x) * (fromNode.x - activeDump.x) + (fromNode.y - activeDump.y) * (fromNode.y - activeDump.y))
            val distTo = sqrt((toNode.x - activeDump.x) * (toNode.x - activeDump.x) + (toNode.y - activeDump.y) * (toNode.y - activeDump.y))
            if (distFrom < activeDump.radius || distTo < activeDump.radius) {
                return false
            }
        }

        return true
    }

    private fun calculateEdgeWeight(
        edge: Edge,
        fromNode: Node,
        toNode: Node,
        profile: AccessibilityProfileType,
        hazardDumps: List<HazardDump>
    ): Float {
        var baseWeight = edge.distance

        when (profile) {
            AccessibilityProfileType.WHEELCHAIR -> {
                if (edge.isRamp) baseWeight *= 0.7f // Strongly prefer ramps
                if (edge.isElevator) baseWeight *= 0.8f // Strongly prefer elevators
            }
            AccessibilityProfileType.BLIND_LOW_VISION -> {
                if (edge.hasStairs) baseWeight *= 3.0f // Penalize stairs for blind users
                if (edge.isRamp) baseWeight *= 0.9f
            }
            else -> {
                if (edge.isElevator) baseWeight += 5f // Small elevator waiting time buffer
            }
        }

        return baseWeight
    }

    private fun createInterFloorEdges(): List<Edge> {
        return listOf(
            Edge("e_inter_e1", "n_e1", "n_f2_e1", 10f, isElevator = true),
            Edge("e_inter_e2", "n_e2", "n_f2_e2", 10f, isElevator = true),
            Edge("e_inter_ramp1", "n_ramp1", "n_f2_ramp1", 15f, isRamp = true),
            Edge("e_inter_stair1", "n_stair1", "n_f2_stair1", 15f, hasStairs = true, isAccessible = false),
            Edge("e_inter_stair2", "n_stair2", "n_f2_stair2", 15f, hasStairs = true, isAccessible = false)
        )
    }

    private fun generateNavigationSteps(
        pathNodes: List<Node>,
        profile: AccessibilityProfileType,
        closedDoors: List<ClosedDoor>,
        hazardDumps: List<HazardDump>,
        userHeading: Float? = null
    ): List<NavigationStep> {
        if (pathNodes.size < 2) return listOf(NavigationStep("You are at your destination", 0, false, StepIconType.DESTINATION))

        val steps = mutableListOf<NavigationStep>()
        for (i in 0 until pathNodes.size - 1) {
            val curr = pathNodes[i]
            val next = pathNodes[i + 1]
            val prev = if (i > 0) pathNodes[i - 1] else null
            val dist = sqrt((next.x - curr.x) * (next.x - curr.x) + (next.y - curr.y) * (next.y - curr.y)).toInt() / 10

            if (curr.floorId != next.floorId) {
                val modeStr = if (curr.type == NodeType.ELEVATOR) "Take Elevator" else "Take Ramp/Stairs"
                steps.add(NavigationStep(modeStr, dist, false, if (curr.type == NodeType.ELEVATOR) StepIconType.ELEVATOR else StepIconType.RAMP))
            } else {
                var direction = "Go straight"
                
                // If we have a previous node, calculate angle from it
                if (prev != null && prev.floorId == curr.floorId && curr.floorId == next.floorId) {
                    val angle1 = Math.atan2((curr.y - prev.y).toDouble(), (curr.x - prev.x).toDouble())
                    val angle2 = Math.atan2((next.y - curr.y).toDouble(), (next.x - curr.x).toDouble())
                    var diff = (angle2 - angle1) * 180 / Math.PI
                    
                    while (diff <= -180) diff += 360
                    while (diff > 180) diff -= 360
                    
                    if (diff > 35) direction = "Turn right"
                    else if (diff < -35) direction = "Turn left"
                } 
                // If no previous node (start of route), use userHeading (if available) to guide them
                else if (prev == null && userHeading != null && curr.floorId == next.floorId) {
                    val angle2 = Math.atan2((next.y - curr.y).toDouble(), (next.x - curr.x).toDouble())
                    var diff = (angle2 - userHeading) * 180 / Math.PI
                    
                    while (diff <= -180) diff += 360
                    while (diff > 180) diff -= 360
                    
                    if (diff > 35) direction = "Turn right"
                    else if (diff < -35) direction = "Turn left"
                }

                when (next.type) {
                    NodeType.ELEVATOR -> steps.add(NavigationStep(if (prev == null) "Head to elevator" else "$direction to elevator", dist, false, StepIconType.ELEVATOR))
                    NodeType.RAMP -> steps.add(NavigationStep("Ramp ahead", dist, false, StepIconType.RAMP))
                    NodeType.STAIR -> {
                        if (profile == AccessibilityProfileType.WHEELCHAIR) {
                            steps.add(NavigationStep("Stairs ahead! Bypassing.", dist, true, StepIconType.STAIRS_WARNING))
                        } else {
                            steps.add(NavigationStep("Staircase ahead", dist, false, StepIconType.STAIRS_WARNING))
                        }
                    }
                    else -> steps.add(NavigationStep(direction, dist.coerceAtLeast(5), false, StepIconType.STRAIGHT))
                }
            }
        }
        steps.add(NavigationStep("You have arrived", 0, false, StepIconType.DESTINATION))
        return steps
    }

    private fun calculateTotalDistance(pathNodes: List<Node>): Float {
        var total = 0f
        for (i in 0 until pathNodes.size - 1) {
            val n1 = pathNodes[i]
            val n2 = pathNodes[i + 1]
            if (n1.floorId == n2.floorId) {
                total += sqrt((n2.x - n1.x) * (n2.x - n1.x) + (n2.y - n1.y) * (n2.y - n1.y)) / 10f
            } else {
                total += 15f // Inter-floor elevation distance
            }
        }
        return total
    }
}
