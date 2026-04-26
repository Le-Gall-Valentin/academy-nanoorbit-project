package fr.vlegall.nanoorbitapplication.domain.model

import java.time.LocalDate

data class Satellite(
    val idSatellite: String,
    val nomSatellite: String,
    val statut: StatutSatellite,
    val formatCubesat: FormatCubeSat,
    val dateLancement: LocalDate? = null,
    val masse: Double? = null,
    val dureeViePrevue: Int? = null,
    val capaciteBatterie: Double? = null,
    val orbite: Orbite? = null
) {
    val canScheduleWindow: Boolean
        get() = statut != StatutSatellite.DESORBITE
}

data class Orbite(
    val idOrbite: String,
    val typeOrbite: TypeOrbite,
    val altitude: Int,
    val inclinaison: Double,
    val periodeOrbitale: Double? = null,
    val excentricite: Double? = null,
    val zoneCouverture: String? = null
)

data class SatelliteSummary(
    val idSatellite: String,
    val nomSatellite: String,
    val statut: StatutSatellite,
    val formatCubesat: FormatCubeSat,
    val dateLancement: LocalDate? = null,
    val orbite: Orbite? = null
) {
    val canScheduleWindow: Boolean
        get() = statut != StatutSatellite.DESORBITE
}