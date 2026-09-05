package com.wayfind.app.data.model

enum class NodeType {
    ENTRANCE,
    RECEPTION,
    CLASSROOM,
    LAB,
    TOILET,
    ELEVATOR,
    STAIR,
    RAMP,
    CORRIDOR
}

data class Node(
    val id: String,
    val floorId: Int,
    val label: String,
    val x: Float,
    val y: Float,
    val type: NodeType
)

data class Edge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distance: Float,
    val isAccessible: Boolean = true,
    val hasStairs: Boolean = false,
    val isElevator: Boolean = false,
    val isRamp: Boolean = false,
    val isNarrow: Boolean = false,
    var isBlocked: Boolean = false
)

data class Wall(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

data class ClosedDoor(
    val id: String,
    val floorId: Int,
    val nodeId1: String,
    val nodeId2: String,
    val label: String,
    var isClosed: Boolean = false
)

data class HazardDump(
    val id: String,
    val floorId: Int,
    val x: Float,
    val y: Float,
    val radius: Float = 25f,
    val label: String,
    var isActive: Boolean = false
)

data class Room(
    val id: String,
    val floorId: Int,
    val name: String,
    val category: String,
    val nodeId: String
)

data class Floor(
    val id: Int,
    val level: Int,
    val name: String,
    val nodes: List<Node>,
    val edges: List<Edge>,
    val walls: List<Wall>,
    val rooms: List<Room>
)

data class Building(
    val id: String,
    val name: String,
    val isSimulated: Boolean = true,
    val floors: List<Floor>
)
