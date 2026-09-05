package com.wayfind.app.data.repository

import com.wayfind.app.data.model.*

class SimulatedBuildingRepository : BuildingRepository {

    private val closedDoors = mutableListOf(
        ClosedDoor(id = "D1", floorId = 1, nodeId1 = "n_door_c101", nodeId2 = "n_c101", label = "Classroom 101 Door", isClosed = false),
        ClosedDoor(id = "D2", floorId = 2, nodeId1 = "n_f2_door_lab104", nodeId2 = "n_f2_lab104", label = "Lab 104 Door", isClosed = true)
    )

    private val hazardDumps = mutableListOf(
        HazardDump(id = "H1", floorId = 1, x = 620f, y = 500f, label = "Maintenance Debris / Dump Area", isActive = false),
        HazardDump(id = "H2", floorId = 2, x = 800f, y = 500f, label = "Construction Waste Blockage", isActive = false)
    )

    // Floor 1 Nodes
    private val floor1Nodes = listOf(
        // Corridor Horizontal Nodes
        Node("n_entrance", 1, "Main Entrance", 100f, 500f, NodeType.ENTRANCE),
        Node("n_corr_w", 1, "West Corridor", 200f, 500f, NodeType.CORRIDOR),
        Node("n_corr_e1", 1, "Elevator 1 Junction", 430f, 500f, NodeType.CORRIDOR),
        Node("n_corr_mid", 1, "Central Corridor", 620f, 500f, NodeType.CORRIDOR),
        Node("n_corr_ramp", 1, "Ramp Junction", 800f, 500f, NodeType.CORRIDOR),
        Node("n_corr_e", 1, "East Corridor", 1080f, 500f, NodeType.CORRIDOR),
        Node("n_corr_e2", 1, "Elevator 2 Junction", 1110f, 500f, NodeType.CORRIDOR),

        // North Room Nodes & Doors
        Node("n_door_rec", 1, "Reception Door", 200f, 400f, NodeType.CORRIDOR),
        Node("n_reception", 1, "Reception", 200f, 250f, NodeType.RECEPTION),
        
        Node("n_door_c101", 1, "C101 Door", 620f, 400f, NodeType.CORRIDOR),
        Node("n_c101", 1, "Classroom 101", 620f, 250f, NodeType.CLASSROOM),
        
        Node("n_door_c102", 1, "C102 Door", 1080f, 400f, NodeType.CORRIDOR),
        Node("n_c102", 1, "Classroom 102", 1080f, 250f, NodeType.CLASSROOM),

        // South Transport Nodes & Lobbies
        Node("n_lobby_e1", 1, "Elevator 1 Lobby", 430f, 600f, NodeType.CORRIDOR),
        Node("n_e1", 1, "Elevator E1", 430f, 750f, NodeType.ELEVATOR),
        Node("n_stair1", 1, "Stairs S1", 370f, 750f, NodeType.STAIR),

        Node("n_lobby_ramp", 1, "Ramp Lobby", 800f, 600f, NodeType.CORRIDOR),
        Node("n_ramp1", 1, "Accessible Ramp R1", 800f, 750f, NodeType.RAMP),

        Node("n_lobby_e2", 1, "Elevator 2 Lobby", 1110f, 600f, NodeType.CORRIDOR),
        Node("n_e2", 1, "Elevator E2", 1110f, 750f, NodeType.ELEVATOR),
        Node("n_stair2", 1, "Stairs S2", 1170f, 750f, NodeType.STAIR)
    )

    private val floor1Edges = mutableListOf(
        // Corridor sequence
        Edge("e1", "n_entrance", "n_corr_w", 10f),
        Edge("e2", "n_corr_w", "n_corr_e1", 23f),
        Edge("e3", "n_corr_e1", "n_corr_mid", 19f),
        Edge("e4", "n_corr_mid", "n_corr_ramp", 18f),
        Edge("e5", "n_corr_ramp", "n_corr_e", 28f),
        Edge("e6", "n_corr_e", "n_corr_e2", 3f),

        // North rooms (Corridor -> Door -> Room)
        Edge("e7", "n_corr_w", "n_door_rec", 10f),
        Edge("e8", "n_door_rec", "n_reception", 15f),

        Edge("e9", "n_corr_mid", "n_door_c101", 10f),
        Edge("e10", "n_door_c101", "n_c101", 15f),

        Edge("e11", "n_corr_e", "n_door_c102", 10f),
        Edge("e12", "n_door_c102", "n_c102", 15f),

        // South transports (Corridor -> Lobby -> Transport)
        Edge("e13", "n_corr_e1", "n_lobby_e1", 10f),
        Edge("e14", "n_lobby_e1", "n_e1", 15f, isElevator = true),
        Edge("e15", "n_lobby_e1", "n_stair1", 16f, hasStairs = true, isAccessible = false),

        Edge("e16", "n_corr_ramp", "n_lobby_ramp", 10f),
        Edge("e17", "n_lobby_ramp", "n_ramp1", 15f, isRamp = true),

        Edge("e18", "n_corr_e2", "n_lobby_e2", 10f),
        Edge("e19", "n_lobby_e2", "n_e2", 15f, isElevator = true),
        Edge("e20", "n_lobby_e2", "n_stair2", 16f, hasStairs = true, isAccessible = false)
    )

    private val floor1Walls = listOf(
        // Outer boundary
        Wall(50f, 100f, 1250f, 100f),
        Wall(1250f, 100f, 1250f, 900f),
        Wall(1250f, 900f, 50f, 900f),
        Wall(50f, 900f, 50f, 100f),

        // North Corridor Wall (y=400) with door gaps
        Wall(50f, 400f, 160f, 400f),
        Wall(240f, 400f, 580f, 400f),
        Wall(660f, 400f, 1040f, 400f),
        Wall(1120f, 400f, 1250f, 400f),

        // South Corridor Wall (y=600) with lobby gaps
        Wall(50f, 600f, 360f, 600f),
        Wall(500f, 600f, 750f, 600f),
        Wall(850f, 600f, 1060f, 600f),
        Wall(1160f, 600f, 1250f, 600f),

        // Room Vertical Partitions
        Wall(350f, 100f, 350f, 400f),
        Wall(850f, 100f, 850f, 400f)
    )

    private val floor1Rooms = listOf(
        Room("r_reception", 1, "Reception", "Service", "n_reception"),
        Room("r_c101", 1, "Classroom 101", "Academic", "n_c101"),
        Room("r_c102", 1, "Classroom 102", "Academic", "n_c102"),
        Room("r_entrance", 1, "Main Entrance", "Exit", "n_entrance")
    )

    // Floor 2 Nodes
    private val floor2Nodes = listOf(
        // Corridor Horizontal Nodes
        Node("n_f2_corr_w", 2, "West Corridor", 200f, 500f, NodeType.CORRIDOR),
        Node("n_f2_corr_e1", 2, "Elevator 1 Junction", 430f, 500f, NodeType.CORRIDOR),
        Node("n_f2_corr_mid", 2, "Central Corridor", 620f, 500f, NodeType.CORRIDOR),
        Node("n_f2_corr_ramp", 2, "Ramp Junction", 800f, 500f, NodeType.CORRIDOR),
        Node("n_f2_corr_e", 2, "East Corridor", 1080f, 500f, NodeType.CORRIDOR),
        Node("n_f2_corr_e2", 2, "Elevator 2 Junction", 1110f, 500f, NodeType.CORRIDOR),

        // North Room Nodes & Doors
        Node("n_f2_door_toilet", 2, "Restroom Door", 200f, 400f, NodeType.CORRIDOR),
        Node("n_f2_toilet", 2, "Accessible Restroom", 200f, 250f, NodeType.TOILET),
        
        Node("n_f2_door_lab103", 2, "Lab 103 Door", 620f, 400f, NodeType.CORRIDOR),
        Node("n_f2_lab103", 2, "Lab 103", 620f, 250f, NodeType.LAB),
        
        Node("n_f2_door_lab104", 2, "Lab 104 Door", 1080f, 400f, NodeType.CORRIDOR),
        Node("n_f2_lab104", 2, "Lab 104", 1080f, 250f, NodeType.LAB),

        // South Transport Nodes & Lobbies
        Node("n_f2_lobby_e1", 2, "Elevator 1 Lobby", 430f, 600f, NodeType.CORRIDOR),
        Node("n_f2_e1", 2, "Elevator E1", 430f, 750f, NodeType.ELEVATOR),
        Node("n_f2_stair1", 2, "Stairs S1", 370f, 750f, NodeType.STAIR),

        Node("n_f2_lobby_ramp", 2, "Ramp Lobby", 800f, 600f, NodeType.CORRIDOR),
        Node("n_f2_ramp1", 2, "Accessible Ramp R1", 800f, 750f, NodeType.RAMP),

        Node("n_f2_lobby_e2", 2, "Elevator 2 Lobby", 1110f, 600f, NodeType.CORRIDOR),
        Node("n_f2_e2", 2, "Elevator E2", 1110f, 750f, NodeType.ELEVATOR),
        Node("n_f2_stair2", 2, "Stairs S2", 1170f, 750f, NodeType.STAIR)
    )

    private val floor2Edges = mutableListOf(
        // Corridor sequence
        Edge("f2_e2", "n_f2_corr_w", "n_f2_corr_e1", 23f),
        Edge("f2_e3", "n_f2_corr_e1", "n_f2_corr_mid", 19f),
        Edge("f2_e4", "n_f2_corr_mid", "n_f2_corr_ramp", 18f),
        Edge("f2_e5", "n_f2_corr_ramp", "n_f2_corr_e", 28f),
        Edge("f2_e6", "n_f2_corr_e", "n_f2_corr_e2", 3f),

        // North rooms (Corridor -> Door -> Room)
        Edge("f2_e7", "n_f2_corr_w", "n_f2_door_toilet", 10f),
        Edge("f2_e8", "n_f2_door_toilet", "n_f2_toilet", 15f),

        Edge("f2_e9", "n_f2_corr_mid", "n_f2_door_lab103", 10f),
        Edge("f2_e10", "n_f2_door_lab103", "n_f2_lab103", 15f),

        Edge("f2_e11", "n_f2_corr_e", "n_f2_door_lab104", 10f),
        Edge("f2_e12", "n_f2_door_lab104", "n_f2_lab104", 15f),

        // South transports (Corridor -> Lobby -> Transport)
        Edge("f2_e13", "n_f2_corr_e1", "n_f2_lobby_e1", 10f),
        Edge("f2_e14", "n_f2_lobby_e1", "n_f2_e1", 15f, isElevator = true),
        Edge("f2_e15", "n_f2_lobby_e1", "n_f2_stair1", 16f, hasStairs = true, isAccessible = false),

        Edge("f2_e16", "n_f2_corr_ramp", "n_f2_lobby_ramp", 10f),
        Edge("f2_e17", "n_f2_lobby_ramp", "n_f2_ramp1", 15f, isRamp = true),

        Edge("f2_e18", "n_f2_corr_e2", "n_f2_lobby_e2", 10f),
        Edge("f2_e19", "n_f2_lobby_e2", "n_f2_e2", 15f, isElevator = true),
        Edge("f2_e20", "n_f2_lobby_e2", "n_f2_stair2", 16f, hasStairs = true, isAccessible = false)
    )

    private val floor2Rooms = listOf(
        Room("r_toilet", 2, "Accessible Restroom", "Facility", "n_f2_toilet"),
        Room("r_lab103", 2, "Lab 103 (Robotics)", "Laboratory", "n_f2_lab103"),
        Room("r_lab104", 2, "Lab 104 (AI Systems)", "Laboratory", "n_f2_lab104")
    )

    override fun getBuilding(): Building {
        val f1 = Floor(1, 1, "Floor 1 — Ground Level", floor1Nodes, floor1Edges, floor1Walls, floor1Rooms)
        val f2 = Floor(2, 2, "Floor 2 — Academic Block", floor2Nodes, floor2Edges, floor1Walls, floor2Rooms) // Sharing walls layout
        return Building("b_simulated", "SIMULATED BUILDING — PROTOTYPE", isSimulated = true, listOf(f1, f2))
    }

    override fun getFloor(floorId: Int): Floor? {
        return getBuilding().floors.find { it.id == floorId }
    }

    override fun getRooms(): List<Room> {
        return floor1Rooms + floor2Rooms
    }

    override fun toggleElevatorState(elevatorId: String, isBlocked: Boolean) {
        val targetNodeName = if (elevatorId == "E1") "n_e1" else "n_e2"
        val targetNodeNameF2 = if (elevatorId == "E1") "n_f2_e1" else "n_f2_e2"

        floor1Edges.forEach { edge ->
            if (edge.toNodeId == targetNodeName || edge.fromNodeId == targetNodeName) {
                edge.isBlocked = isBlocked
            }
        }
        floor2Edges.forEach { edge ->
            if (edge.toNodeId == targetNodeNameF2 || edge.fromNodeId == targetNodeNameF2) {
                edge.isBlocked = isBlocked
            }
        }
    }

    override fun toggleClosedDoor(doorId: String, isClosed: Boolean) {
        closedDoors.find { it.id == doorId }?.isClosed = isClosed
    }

    override fun toggleHazardDump(dumpId: String, isActive: Boolean) {
        hazardDumps.find { it.id == dumpId }?.isActive = isActive
    }

    override fun getClosedDoors(floorId: Int): List<ClosedDoor> {
        return closedDoors.filter { it.floorId == floorId }
    }

    override fun getHazardDumps(floorId: Int): List<HazardDump> {
        return hazardDumps.filter { it.floorId == floorId }
    }
}
