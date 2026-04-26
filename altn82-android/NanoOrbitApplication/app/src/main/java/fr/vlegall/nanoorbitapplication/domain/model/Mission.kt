package fr.vlegall.nanoorbitapplication.domain.model

import java.time.LocalDate

data class Mission(
    val idMission: String,
    val nomMission: String,
    val objectif: String? = null,
    val dateDebut: LocalDate? = null,
    val statutMission: String,
    val dateFin: LocalDate? = null,
    val zoneGeoCible: String? = null
)