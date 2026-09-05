package com.wayfind.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wayfind.app.data.model.*
import com.wayfind.app.data.repository.BuildingRepository
import com.wayfind.app.data.repository.SimulatedBuildingRepository
import com.wayfind.app.domain.CollisionSystem
import com.wayfind.app.domain.HapticManager
import com.wayfind.app.domain.RouteEngine
import com.wayfind.app.domain.TTSManager
import com.wayfind.app.domain.VoiceAssistantManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    WELCOME,
    PROFILE_SELECTION,
    MAP_NAVIGATION
}

class WayfindViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BuildingRepository = SimulatedBuildingRepository()
    private val routeEngine = RouteEngine()
    private val collisionSystem = CollisionSystem()

    private val ttsManager = TTSManager(application)
    private val hapticManager = HapticManager(application)
    val announcementManager = AnnouncementManager(ttsManager, hapticManager)
    val voiceAssistantManager = VoiceAssistantManager(application, ttsManager)

    private val _currentScreen = MutableStateFlow(AppScreen.WELCOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    private val _selectedProfile = MutableStateFlow(AccessibilityProfileType.WHEELCHAIR)
    val selectedProfile: StateFlow<AccessibilityProfileType> = _selectedProfile

    private val _building = MutableStateFlow(repository.getBuilding())
    val building: StateFlow<Building> = _building

    private val _activeFloorId = MutableStateFlow(1)
    val activeFloorId: StateFlow<Int> = _activeFloorId

    private val _userX = MutableStateFlow(100f)
    val userX: StateFlow<Float> = _userX

    private val _userY = MutableStateFlow(500f)
    val userY: StateFlow<Float> = _userY

    private val _userFloorId = MutableStateFlow(1)
    val userFloorId: StateFlow<Int> = _userFloorId

    private val _selectedDestination = MutableStateFlow<Room?>(null)
    val selectedDestination: StateFlow<Room?> = _selectedDestination

    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute

    // Environmental state toggles
    private val _isElevatorE1Blocked = MutableStateFlow(false)
    val isElevatorE1Blocked: StateFlow<Boolean> = _isElevatorE1Blocked

    private val _isElevatorE2Blocked = MutableStateFlow(false)
    val isElevatorE2Blocked: StateFlow<Boolean> = _isElevatorE2Blocked

    private val _closedDoors = MutableStateFlow(repository.getClosedDoors(1))
    val closedDoors: StateFlow<List<ClosedDoor>> = _closedDoors

    private val _hazardDumps = MutableStateFlow(repository.getHazardDumps(1))
    val hazardDumps: StateFlow<List<HazardDump>> = _hazardDumps

    private val _isDevPanelVisible = MutableStateFlow(false)
    val isDevPanelVisible: StateFlow<Boolean> = _isDevPanelVisible

    private val _isDemoRunning = MutableStateFlow(false)
    val isDemoRunning: StateFlow<Boolean> = _isDemoRunning

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectProfile(profile: AccessibilityProfileType) {
        _selectedProfile.value = profile
    }

    fun selectFloor(floorId: Int) {
        _activeFloorId.value = floorId
        _closedDoors.value = repository.getClosedDoors(floorId)
        _hazardDumps.value = repository.getHazardDumps(floorId)
    }

    fun setDestination(room: Room) {
        _selectedDestination.value = room
        recalculateRoute()
        // Announce first navigation step to blind users after route is ready
        if (_selectedProfile.value == AccessibilityProfileType.BLIND_LOW_VISION) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                _currentRoute.value?.steps?.firstOrNull()?.let { step ->
                    voiceAssistantManager.announceStep(step.instruction)
                }
            }
        }
    }

    fun startVoiceNavigationForBlind() {
        val rooms = repository.getRooms()
        voiceAssistantManager.initialize(rooms)
        voiceAssistantManager.greetUser()
    }

    fun onDoubleTapForVoice() {
        val rooms = repository.getRooms()
        voiceAssistantManager.initialize(rooms)
        voiceAssistantManager.onDoubleTap { room ->
            setDestination(room)
        }
    }

    fun onMicPermissionDenied() {
        voiceAssistantManager.announceMicPermissionDenied()
    }

    private var lastRouteRecalcTime = 0L
    private var lastUserHeading: Float? = null

    fun handleJoystickMove(dx: Float, dy: Float, speedMultiplier: Float = 4f) {
        if (dx == 0f && dy == 0f) return

        // Update the user's current facing direction (heading in radians)
        lastUserHeading = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

        val currentFloor = repository.getFloor(_userFloorId.value) ?: return
        val currentClosed = repository.getClosedDoors(_userFloorId.value)
        val currentDumps = repository.getHazardDumps(_userFloorId.value)

        val targetX = _userX.value + (dx * speedMultiplier)
        val targetY = _userY.value + (dy * speedMultiplier)

        val (nextX, nextY) = collisionSystem.resolvePosition(
            currentX = _userX.value,
            currentY = _userY.value,
            targetX = targetX,
            targetY = targetY,
            walls = currentFloor.walls,
            closedDoors = currentClosed,
            hazardDumps = currentDumps
        )

        _userX.value = nextX
        _userY.value = nextY

        // Auto-floor transition check near Elevators, Stairs, and Ramps
        checkFloorTransition(nextX, nextY)

        // Throttle route recalculation during movement to check for step progression
        val currentTime = System.currentTimeMillis()
        if (_selectedDestination.value != null && (currentTime - lastRouteRecalcTime > 500)) {
            lastRouteRecalcTime = currentTime

            val oldInstruction = _currentRoute.value?.steps?.firstOrNull()?.instruction
            recalculateRoute()
            val newInstruction = _currentRoute.value?.steps?.firstOrNull()?.instruction

            if (newInstruction != null && newInstruction != oldInstruction) {
                // If the new instruction indicates arrival, clear the route and announce success
                if (newInstruction == "You are at your destination") {
                    announcementManager.triggerAnnouncement(
                        type = AnnouncementType.DESTINATION_REACHED,
                        title = "🎉 Destination Reached!",
                        message = "You have arrived safely at ${_selectedDestination.value?.name}.",
                        profile = _selectedProfile.value
                    )
                    if (_selectedProfile.value == AccessibilityProfileType.BLIND_LOW_VISION) {
                        voiceAssistantManager.announceStep("You have arrived at your destination.")
                    }
                    _selectedDestination.value = null
                    _currentRoute.value = null
                } 
                // Otherwise, announce the next turn for blind users
                else if (_selectedProfile.value == AccessibilityProfileType.BLIND_LOW_VISION) {
                    voiceAssistantManager.announceStep(newInstruction)
                }
            }
        }
    }

    private fun checkFloorTransition(x: Float, y: Float) {
        val currFloorNodes = repository.getFloor(_userFloorId.value)?.nodes ?: return
        for (node in currFloorNodes) {
            if (node.type == NodeType.ELEVATOR || node.type == NodeType.STAIR || node.type == NodeType.RAMP) {
                // Prevent wheelchair users from automatically taking stairs
                if (node.type == NodeType.STAIR && _selectedProfile.value == AccessibilityProfileType.WHEELCHAIR) {
                    continue
                }

                val distSq = (x - node.x) * (x - node.x) + (y - node.y) * (y - node.y)
                if (distSq < 400f) { // Within transition zone
                    val targetFloor = if (_userFloorId.value == 1) 2 else 1
                    if (_activeFloorId.value != targetFloor) {
                        _userFloorId.value = targetFloor
                        _activeFloorId.value = targetFloor
                        
                        // Set user near the corresponding node on the new floor
                        val newFloorNode = repository.getFloor(targetFloor)?.nodes?.find { 
                            it.type == node.type && it.label.contains(node.label.takeLast(2)) 
                        }
                        
                        if (newFloorNode != null) {
                            _userX.value = newFloorNode.x
                            _userY.value = newFloorNode.y - 150f
                        }
                        
                        val transitionName = when (node.type) {
                            NodeType.ELEVATOR -> "Elevator"
                            NodeType.STAIR -> "Stairs"
                            NodeType.RAMP -> "Ramp"
                            else -> "Floor"
                        }
                        
                        announcementManager.triggerAnnouncement(
                            type = AnnouncementType.REROUTE_SUCCESS,
                            title = "$transitionName Floor Transition",
                            message = "Arrived at Floor $targetFloor via $transitionName",
                            profile = _selectedProfile.value
                        )
                        recalculateRoute()
                    }
                    break
                }
            }
        }
    }

    fun recalculateRoute(userHeading: Float? = lastUserHeading) {
        val dest = _selectedDestination.value ?: return
        val building = _building.value
        val allNodes = building.floors.flatMap { it.nodes }

        // Nearest start node
        val startNode = allNodes.filter { it.floorId == _userFloorId.value }
            .minByOrNull { (it.x - _userX.value) * (it.x - _userX.value) + (it.y - _userY.value) * (it.y - _userY.value) }
            ?: return

        val destNode = allNodes.find { it.id == dest.nodeId } ?: return

        val route = routeEngine.calculateRoute(
            startNode = startNode,
            destNode = destNode,
            profile = _selectedProfile.value,
            building = building,
            closedDoors = repository.getClosedDoors(_userFloorId.value) + repository.getClosedDoors(if (_userFloorId.value == 1) 2 else 1),
            hazardDumps = repository.getHazardDumps(_userFloorId.value) + repository.getHazardDumps(if (_userFloorId.value == 1) 2 else 1),
            userHeading = userHeading
        )

        _currentRoute.value = route
    }

    fun toggleElevatorE1() {
        val newState = !_isElevatorE1Blocked.value
        _isElevatorE1Blocked.value = newState
        repository.toggleElevatorState("E1", newState)

        if (newState) {
            announcementManager.triggerAnnouncement(
                type = AnnouncementType.ELEVATOR_FAILURE,
                title = "⚠️ Elevator E1 Unavailable",
                message = "Elevator E1 has experienced a structural fault. Recalculating accessible route...",
                profile = _selectedProfile.value
            )
        } else {
            announcementManager.triggerAnnouncement(
                type = AnnouncementType.REROUTE_SUCCESS,
                title = "Elevator E1 Restored",
                message = "Elevator E1 is operational. Updating route...",
                profile = _selectedProfile.value
            )
        }
        recalculateRoute()
    }

    fun toggleElevatorE2() {
        val newState = !_isElevatorE2Blocked.value
        _isElevatorE2Blocked.value = newState
        repository.toggleElevatorState("E2", newState)
        recalculateRoute()
    }

    fun toggleDoor(doorId: String) {
        val doors = repository.getClosedDoors(_activeFloorId.value)
        val door = doors.find { it.id == doorId }
        if (door != null) {
            val newClosed = !door.isClosed
            repository.toggleClosedDoor(doorId, newClosed)
            _closedDoors.value = repository.getClosedDoors(_activeFloorId.value)
            if (newClosed) {
                announcementManager.triggerAnnouncement(
                    type = AnnouncementType.CLOSED_DOOR_AHEAD,
                    title = "Door Closed",
                    message = "${door.label} is closed. Recalculating route around door.",
                    profile = _selectedProfile.value
                )
            }
            recalculateRoute()
        }
    }

    fun toggleHazardDump(dumpId: String) {
        val dumps = repository.getHazardDumps(_activeFloorId.value)
        val dump = dumps.find { it.id == dumpId }
        if (dump != null) {
            val newActive = !dump.isActive
            repository.toggleHazardDump(dumpId, newActive)
            _hazardDumps.value = repository.getHazardDumps(_activeFloorId.value)
            if (newActive) {
                announcementManager.triggerAnnouncement(
                    type = AnnouncementType.HAZARD_DUMP_DETECTED,
                    title = "⚠️ Hazard / Dump Detected",
                    message = "Debris dump active at ${dump.label}. Re-routing accessible pathway.",
                    profile = _selectedProfile.value
                )
            }
            recalculateRoute()
        }
    }

    fun toggleDevPanel() {
        _isDevPanelVisible.value = !_isDevPanelVisible.value
    }

    fun runDemoSequence() {
        viewModelScope.launch {
            _isDemoRunning.value = true

            // 1. Select Wheelchair Profile
            _selectedProfile.value = AccessibilityProfileType.WHEELCHAIR
            _currentScreen.value = AppScreen.MAP_NAVIGATION

            // 2. Reset Elevator state
            if (_isElevatorE1Blocked.value) toggleElevatorE1()

            // 3. Select Destination (Lab 103 on Floor 2)
            val lab103 = repository.getRooms().find { it.id == "r_lab103" }
            if (lab103 != null) setDestination(lab103)

            delay(1500)

            // 4. Move user closer to Elevator E1
            _userX.value = 350f
            _userY.value = 450f
            recalculateRoute()

            delay(2000)

            // 5. Trigger Elevator E1 Failure!
            toggleElevatorE1()

            delay(3000)

            // 6. Navigate user via Ramp R1 towards Elevator E2
            _userX.value = 620f
            _userY.value = 500f
            recalculateRoute()

            delay(2500)

            // 7. Transition user to Floor 2 near Elevator E2
            _userFloorId.value = 2
            _activeFloorId.value = 2
            _userX.value = 780f
            _userY.value = 350f
            recalculateRoute()

            delay(2000)

            // 8. Reach Lab 103 on Floor 2
            _userX.value = 300f
            _userY.value = 250f

            announcementManager.triggerAnnouncement(
                type = AnnouncementType.DESTINATION_REACHED,
                title = "🎉 Destination Reached!",
                message = "You have arrived safely at Lab 103 (Robotics Lab).",
                profile = _selectedProfile.value
            )

            _isDemoRunning.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistantManager.shutdown()
        ttsManager.shutdown()
    }
}
