package fr.vlegall.nanoorbitapplication.domain.model

data class Dashboard(
    val totalSatellites: Long,
    val satellitesByStatut: Map<String, Long>,
    val totalStations: Long,
    val stationsByStatut: Map<String, Long>,
    val fenetresPlanifiees: Long,
    val fenetresRealisees: Long,
    val totalMissions: Long,
    val missionsByStatut: Map<String, Long>
) {
    val satellitesOperationnels: Long
        get() = satellitesByStatut[StatutSatellite.OPERATIONNEL.label] ?: 0L
}