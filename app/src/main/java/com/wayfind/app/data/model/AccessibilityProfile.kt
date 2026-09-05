package com.wayfind.app.data.model

enum class AccessibilityProfileType(
    val title: String,
    val description: String,
    val features: List<String>
) {
    WHEELCHAIR(
        title = "Wheelchair",
        description = "Optimized for physical accessibility and minimum elevation barriers.",
        features = listOf("Avoid stairs", "Prefer ramps & elevators", "Avoid narrow corridors", "Accessible doors")
    ),
    BLIND_LOW_VISION(
        title = "Blind / Low Vision",
        description = "Provides auditory cues, spoken announcements, and tactile feedback.",
        features = listOf("Audio guidance (TTS)", "Haptic vibration turns", "Hazard & obstacle warnings")
    ),
    HEARING_IMPAIRMENT(
        title = "Hearing Impairment",
        description = "High contrast visual turn-by-turn guidance and prominent banners.",
        features = listOf("Visual turn directions", "Visual obstacle alerts", "Reroute notification banners")
    ),
    GENERAL(
        title = "General User",
        description = "Calculates the fastest practical route through the building.",
        features = listOf("Fastest route", "All practical pathways")
    ),
    TEMPORARY_MOBILITY(
        title = "Temporary Mobility Limitation",
        description = "Minimizes strain by favoring elevators and gentle ramps.",
        features = listOf("Minimum barriers", "Avoid stairs where possible", "Shortest walking distances")
    )
}
