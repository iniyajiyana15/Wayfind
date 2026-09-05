package com.wayfind.app.data.model

enum class StepIconType {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    RAMP,
    ELEVATOR,
    STAIRS_WARNING,
    DESTINATION,
    HAZARD_WARNING
}

data class NavigationStep(
    val instruction: String,
    val distanceMeters: Int,
    val isWarning: Boolean = false,
    val iconType: StepIconType = StepIconType.STRAIGHT
)

data class Route(
    val pathNodes: List<Node>,
    val totalDistanceMeters: Float,
    val estimatedTimeMinutes: Int,
    val steps: List<NavigationStep>,
    val isAccessibleForProfile: Boolean = true,
    val profileType: AccessibilityProfileType
)

enum class AnnouncementType {
    ELEVATOR_FAILURE,
    REROUTE_SUCCESS,
    HAZARD_DUMP_DETECTED,
    CLOSED_DOOR_AHEAD,
    DESTINATION_REACHED
}

data class AnnouncementEvent(
    val id: String,
    val type: AnnouncementType,
    val title: String,
    val message: String,
    val isUrgent: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
