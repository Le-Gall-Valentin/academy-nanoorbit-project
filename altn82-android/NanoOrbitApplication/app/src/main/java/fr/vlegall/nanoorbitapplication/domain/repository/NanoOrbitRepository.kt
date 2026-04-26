package fr.vlegall.nanoorbitapplication.domain.repository

import fr.vlegall.nanoorbitapplication.domain.model.Dashboard
import fr.vlegall.nanoorbitapplication.domain.model.Embarquement
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.FenetreComCreateRequest
import fr.vlegall.nanoorbitapplication.domain.model.HistoriqueStatut
import fr.vlegall.nanoorbitapplication.domain.model.Participation
import fr.vlegall.nanoorbitapplication.domain.model.Satellite
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import kotlinx.coroutines.flow.Flow

interface NanoOrbitRepository {

    val isOffline: Flow<Boolean>

    suspend fun getDashboard(): Result<Dashboard>

    fun getSatellitesStream(statut: StatutSatellite? = null): Flow<List<SatelliteSummary>>

    suspend fun getSatelliteById(id: String): Result<Satellite>

    suspend fun updateStatutSatellite(id: String, statut: StatutSatellite): Result<Satellite>

    suspend fun signalerAnomalie(id: String, description: String): Result<Unit>

    suspend fun getInstruments(idSatellite: String): Result<List<Embarquement>>

    suspend fun getMissions(idSatellite: String): Result<List<Participation>>

    suspend fun getHistoriqueStatut(idSatellite: String): Result<List<HistoriqueStatut>>

    suspend fun getFenetresBySatellite(idSatellite: String): Result<List<FenetreCom>>

    fun getFenetresStream(
        statut: String? = null,
        codeStation: String? = null,
        idSatellite: String? = null
    ): Flow<List<FenetreCom>>

    suspend fun getFenetreById(id: Long): Result<FenetreCom>

    suspend fun createFenetre(request: FenetreComCreateRequest): Result<FenetreCom>

    suspend fun realiserFenetre(id: Long, volumeDonnees: Double): Result<FenetreCom>

    suspend fun deleteFenetre(id: Long): Result<Unit>

    fun getStationsStream(): Flow<List<StationSol>>

    suspend fun getStations(statut: String? = null): Result<List<StationSol>>

    suspend fun getStationByCode(codeStation: String): Result<StationSol>

    suspend fun getFenetresByStation(codeStation: String): Result<List<FenetreCom>>

    fun getLastSyncTimestamp(): Flow<Long?>

    suspend fun hasPendingOperations(): Boolean

    suspend fun syncPendingOperations(): Result<Int>

    suspend fun forceRefresh(): Result<Unit>

    fun getFavoriteIds(): Flow<Set<String>>

    suspend fun toggleFavorite(id: String)
}