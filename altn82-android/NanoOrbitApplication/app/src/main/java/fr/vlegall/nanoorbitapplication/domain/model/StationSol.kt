package fr.vlegall.nanoorbitapplication.domain.model

data class StationSol(
    val codeStation: String,
    val nomStation: String,
    val latitude: Double,
    val longitude: Double,
    val diametreAntenne: Double? = null,
    val bandeFrequence: String? = null,
    val debitMax: Double? = null,
    val statut: StatutStation
) {
    val canReceiveWindow: Boolean
        get() = statut != StatutStation.MAINTENANCE && statut != StatutStation.INACTIVE
}