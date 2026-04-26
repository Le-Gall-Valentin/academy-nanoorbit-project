package fr.vlegall.nanoorbitapplication.domain.model

import java.time.LocalDateTime

data class FenetreCom(
    val idFenetre: Long,
    val datetimeDebut: LocalDateTime,
    val duree: Int,
    val elevationMax: Double,
    val volumeDonnees: Double? = null,
    val statut: StatutFenetre,
    val idSatellite: String,
    val nomSatellite: String? = null,
    val codeStation: String,
    val nomStation: String? = null
) {
    companion object {
        const val DUREE_MAX_SECONDES = 900
        const val DUREE_MIN_SECONDES = 1
    }
}

data class FenetreComCreateRequest(
    val idSatellite: String,
    val codeStation: String,
    val datetimeDebut: LocalDateTime,
    val duree: Int,
    val elevationMax: Double
)