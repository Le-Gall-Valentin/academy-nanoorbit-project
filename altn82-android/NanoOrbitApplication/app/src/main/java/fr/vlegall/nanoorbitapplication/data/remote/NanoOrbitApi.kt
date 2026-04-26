package fr.vlegall.nanoorbitapplication.data.remote

import fr.vlegall.nanoorbitapplication.data.remote.dto.DashboardDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.EmbarquementDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.FenetreComCreateRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.FenetreComDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.FenetreComRealiserRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.HistoriqueStatutDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.ParticipationDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteAnomalieRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteStatutRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteSummaryDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.StationSolDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NanoOrbitApi {

    @GET("api/dashboard")
    suspend fun getDashboard(): DashboardDto

    @GET("api/satellites")
    suspend fun getSatellites(
        @Query("statut") statut: String? = null
    ): List<SatelliteSummaryDto>

    @GET("api/satellites/{id}")
    suspend fun getSatelliteById(
        @Path("id") id: String
    ): SatelliteDto

    @PATCH("api/satellites/{id}/statut")
    suspend fun updateStatut(
        @Path("id") id: String,
        @Body request: SatelliteStatutRequestDto
    ): SatelliteDto

    @POST("api/satellites/{id}/anomalies")
    suspend fun signalerAnomalie(
        @Path("id") id: String,
        @Body request: SatelliteAnomalieRequestDto
    )

    @GET("api/satellites/{id}/instruments")
    suspend fun getInstruments(
        @Path("id") id: String
    ): List<EmbarquementDto>

    @GET("api/satellites/{id}/missions")
    suspend fun getMissions(
        @Path("id") id: String
    ): List<ParticipationDto>

    @GET("api/satellites/{id}/historique-statut")
    suspend fun getHistoriqueStatut(
        @Path("id") id: String
    ): List<HistoriqueStatutDto>

    @GET("api/satellites/{id}/fenetres")
    suspend fun getFenetresBySatellite(
        @Path("id") id: String
    ): List<FenetreComDto>

    @GET("api/fenetres")
    suspend fun getFenetres(
        @Query("statut") statut: String? = null,
        @Query("codeStation") codeStation: String? = null,
        @Query("idSatellite") idSatellite: String? = null
    ): List<FenetreComDto>

    @GET("api/fenetres/{id}")
    suspend fun getFenetreById(
        @Path("id") id: Long
    ): FenetreComDto

    @POST("api/fenetres")
    suspend fun createFenetre(
        @Body request: FenetreComCreateRequestDto
    ): FenetreComDto

    @PATCH("api/fenetres/{id}/realiser")
    suspend fun realiserFenetre(
        @Path("id") id: Long,
        @Body request: FenetreComRealiserRequestDto
    ): FenetreComDto

    @DELETE("api/fenetres/{id}")
    suspend fun deleteFenetre(
        @Path("id") id: Long
    )

    @GET("api/stations-sol")
    suspend fun getStations(
        @Query("statut") statut: String? = null
    ): List<StationSolDto>

    @GET("api/stations-sol/{codeStation}")
    suspend fun getStationByCode(
        @Path("codeStation") codeStation: String
    ): StationSolDto

    @GET("api/stations-sol/{codeStation}/fenetres")
    suspend fun getFenetresByStation(
        @Path("codeStation") codeStation: String
    ): List<FenetreComDto>
}