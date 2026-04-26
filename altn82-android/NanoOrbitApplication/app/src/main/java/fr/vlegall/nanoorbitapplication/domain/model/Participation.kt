package fr.vlegall.nanoorbitapplication.domain.model

data class Participation(
    val idSatellite: String,
    val nomSatellite: String,
    val idMission: String,
    val nomMission: String,
    val roleSatellite: String,
    val commentaire: String? = null
)