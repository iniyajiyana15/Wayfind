package com.wayfind.app.data.model

import com.wayfind.app.domain.HapticManager
import com.wayfind.app.domain.TTSManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AnnouncementManager(
    private val ttsManager: TTSManager?,
    private val hapticManager: HapticManager?
) {
    private val _activeAnnouncement = MutableStateFlow<AnnouncementEvent?>(null)
    val activeAnnouncement: StateFlow<AnnouncementEvent?> = _activeAnnouncement

    fun triggerAnnouncement(
        type: AnnouncementType,
        title: String,
        message: String,
        profile: AccessibilityProfileType
    ) {
        val event = AnnouncementEvent(
            id = "ann_${System.currentTimeMillis()}",
            type = type,
            title = title,
            message = message
        )
        _activeAnnouncement.value = event

        // TTS for Blind or General mode
        if (profile == AccessibilityProfileType.BLIND_LOW_VISION || profile == AccessibilityProfileType.GENERAL) {
            ttsManager?.speak("$title. $message")
        }

        // Haptic feedback for Blind / Low Vision or Hearing Impairment
        if (profile == AccessibilityProfileType.BLIND_LOW_VISION || profile == AccessibilityProfileType.HEARING_IMPAIRMENT) {
            when (type) {
                AnnouncementType.ELEVATOR_FAILURE, AnnouncementType.HAZARD_DUMP_DETECTED -> hapticManager?.vibrateHazardAlert()
                AnnouncementType.REROUTE_SUCCESS -> hapticManager?.vibrateTurnRight()
                AnnouncementType.DESTINATION_REACHED -> hapticManager?.vibrateDestinationReached()
                else -> hapticManager?.vibrateTurnLeft()
            }
        }
    }

    fun dismissAnnouncement() {
        _activeAnnouncement.value = null
    }
}
