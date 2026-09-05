package com.wayfind.app.data.repository

import com.wayfind.app.data.model.*

interface BuildingRepository {
    fun getBuilding(): Building
    fun getFloor(floorId: Int): Floor?
    fun getRooms(): List<Room>
    fun toggleElevatorState(elevatorId: String, isBlocked: Boolean)
    fun toggleClosedDoor(doorId: String, isClosed: Boolean)
    fun toggleHazardDump(dumpId: String, isActive: Boolean)
    fun getClosedDoors(floorId: Int): List<ClosedDoor>
    fun getHazardDumps(floorId: Int): List<HazardDump>
}
