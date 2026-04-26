package fr.vlegall.nanoorbitapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrbiteDto(
    @SerializedName("idOrbite") val idOrbite: String?,
    @SerializedName("typeOrbite") val typeOrbite: String?,
    @SerializedName("altitude") val altitude: Int?,
    @SerializedName("inclinaison") val inclinaison: Double?,
    @SerializedName("periodeOrbitale") val periodeOrbitale: Double?,
    @SerializedName("excentricite") val excentricite: Double?,
    @SerializedName("zoneCouverture") val zoneCouverture: String?
)

data class SatelliteSummaryDto(
    @SerializedName("idSatellite") val idSatellite: String?,
    @SerializedName("nomSatellite") val nomSatellite: String?,
    @SerializedName("statutOperationnel") val statutOperationnel: String?,
    @SerializedName("formatCubesat") val formatCubesat: String?,
    @SerializedName("dateLancement") val dateLancement: String?,
    @SerializedName("orbite") val orbite: OrbiteDto?
)

data class SatelliteDto(
    @SerializedName("idSatellite") val idSatellite: String?,
    @SerializedName("nomSatellite") val nomSatellite: String?,
    @SerializedName("statutOperationnel") val statutOperationnel: String?,
    @SerializedName("formatCubesat") val formatCubesat: String?,
    @SerializedName("dateLancement") val dateLancement: String?,
    @SerializedName("masse") val masse: Double?,
    @SerializedName("dureeViePrevue") val dureeViePrevue: Int?,
    @SerializedName("capaciteBatterie") val capaciteBatterie: Double?,
    @SerializedName("orbite") val orbite: OrbiteDto?
)

data class FenetreComDto(
    @SerializedName("idFenetre") val idFenetre: Long?,
    @SerializedName("datetimeDebut") val datetimeDebut: String?,
    @SerializedName("duree") val duree: Int?,
    @SerializedName("elevationMax") val elevationMax: Double?,
    @SerializedName("volumeDonnees") val volumeDonnees: Double?,
    @SerializedName("statut") val statut: String?,
    @SerializedName("idSatellite") val idSatellite: String?,
    @SerializedName("nomSatellite") val nomSatellite: String?,
    @SerializedName("codeStation") val codeStation: String?,
    @SerializedName("nomStation") val nomStation: String?
)

data class StationSolDto(
    @SerializedName("codeStation") val codeStation: String?,
    @SerializedName("nomStation") val nomStation: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("diametreAntenne") val diametreAntenne: Double?,
    @SerializedName("bandeFrequence") val bandeFrequence: String?,
    @SerializedName("debitMax") val debitMax: Double?,
    @SerializedName("statut") val statut: String?
)

data class EmbarquementDto(
    @SerializedName("refInstrument") val refInstrument: String?,
    @SerializedName("typeInstrument") val typeInstrument: String?,
    @SerializedName("modele") val modele: String?,
    @SerializedName("dateIntegration") val dateIntegration: String?,
    @SerializedName("etatFonctionnement") val etatFonctionnement: String?,
    @SerializedName("commentaire") val commentaire: String?
)

data class ParticipationDto(
    @SerializedName("idSatellite") val idSatellite: String?,
    @SerializedName("nomSatellite") val nomSatellite: String?,
    @SerializedName("idMission") val idMission: String?,
    @SerializedName("nomMission") val nomMission: String?,
    @SerializedName("roleSatellite") val roleSatellite: String?,
    @SerializedName("commentaire") val commentaire: String?
)

data class HistoriqueStatutDto(
    @SerializedName("idHistorique") val idHistorique: Long?,
    @SerializedName("statut") val statut: String?,
    @SerializedName("timestamp") val timestamp: String?
)

data class DashboardDto(
    @SerializedName("totalSatellites") val totalSatellites: Long?,
    @SerializedName("satellitesByStatut") val satellitesByStatut: Map<String, Long>?,
    @SerializedName("totalStations") val totalStations: Long?,
    @SerializedName("stationsByStatut") val stationsByStatut: Map<String, Long>?,
    @SerializedName("fenetresPlanifiees") val fenetresPlanifiees: Long?,
    @SerializedName("fenetresRealisees") val fenetresRealisees: Long?,
    @SerializedName("totalMissions") val totalMissions: Long?,
    @SerializedName("missionsByStatut") val missionsByStatut: Map<String, Long>?
)

data class SatelliteStatutRequestDto(
    @SerializedName("statutOperationnel") val statutOperationnel: String
)

data class SatelliteAnomalieRequestDto(
    @SerializedName("description") val description: String
)

data class FenetreComCreateRequestDto(
    @SerializedName("idSatellite") val idSatellite: String,
    @SerializedName("codeStation") val codeStation: String,
    @SerializedName("datetimeDebut") val datetimeDebut: String,
    @SerializedName("duree") val duree: Int,
    @SerializedName("elevationMax") val elevationMax: Double
)

data class FenetreComRealiserRequestDto(
    @SerializedName("volumeDonnees") val volumeDonnees: Double
)