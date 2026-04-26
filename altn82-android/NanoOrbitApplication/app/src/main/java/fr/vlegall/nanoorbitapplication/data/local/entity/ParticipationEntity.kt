package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity

@Entity(tableName = "participations", primaryKeys = ["idSatellite", "idMission"])
data class ParticipationEntity(
    val idSatellite: String,
    val nomSatellite: String,
    val idMission: String,
    val nomMission: String,
    val roleSatellite: String,
    val commentaire: String? = null
)