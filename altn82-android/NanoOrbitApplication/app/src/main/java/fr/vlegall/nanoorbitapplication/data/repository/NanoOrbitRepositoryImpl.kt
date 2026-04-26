package fr.vlegall.nanoorbitapplication.data.repository

import com.google.gson.Gson
import fr.vlegall.nanoorbitapplication.data.local.FavoritesDataStore
import fr.vlegall.nanoorbitapplication.data.local.dao.EmbarquementDao
import fr.vlegall.nanoorbitapplication.data.local.dao.FenetreDao
import fr.vlegall.nanoorbitapplication.data.local.dao.HistoriqueStatutDao
import fr.vlegall.nanoorbitapplication.data.local.dao.ParticipationDao
import fr.vlegall.nanoorbitapplication.data.local.dao.PendingOperationDao
import fr.vlegall.nanoorbitapplication.data.local.dao.SatelliteDao
import fr.vlegall.nanoorbitapplication.data.local.dao.StationDao
import fr.vlegall.nanoorbitapplication.data.local.entity.EmbarquementEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.FenetreEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.HistoriqueStatutEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.ParticipationEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.PendingOperationEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.PendingOperationType
import fr.vlegall.nanoorbitapplication.data.local.entity.SatelliteEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.StationEntity
import fr.vlegall.nanoorbitapplication.data.remote.NanoOrbitApi
import fr.vlegall.nanoorbitapplication.data.remote.dto.FenetreComCreateRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.FenetreComDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.FenetreComRealiserRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteAnomalieRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteStatutRequestDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.SatelliteSummaryDto
import fr.vlegall.nanoorbitapplication.data.remote.dto.StationSolDto
import fr.vlegall.nanoorbitapplication.domain.model.Dashboard
import fr.vlegall.nanoorbitapplication.domain.model.Embarquement
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.FenetreComCreateRequest
import fr.vlegall.nanoorbitapplication.domain.model.FormatCubeSat
import fr.vlegall.nanoorbitapplication.domain.model.HistoriqueStatut
import fr.vlegall.nanoorbitapplication.domain.model.Orbite
import fr.vlegall.nanoorbitapplication.domain.model.Participation
import fr.vlegall.nanoorbitapplication.domain.model.Satellite
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StatutFenetre
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.domain.model.StatutStation
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import fr.vlegall.nanoorbitapplication.domain.model.TypeOrbite
import fr.vlegall.nanoorbitapplication.domain.repository.NanoOrbitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class NanoOrbitRepositoryImpl(
    private val api: NanoOrbitApi,
    private val satelliteDao: SatelliteDao,
    private val fenetreDao: FenetreDao,
    private val pendingOperationDao: PendingOperationDao,
    private val stationDao: StationDao,
    private val embarquementDao: EmbarquementDao,
    private val participationDao: ParticipationDao,
    private val historiqueStatutDao: HistoriqueStatutDao,
    private val favoritesDataStore: FavoritesDataStore
) : NanoOrbitRepository {

    private val gson = Gson()
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)

    private val _isOffline = MutableStateFlow(false)
    override val isOffline: Flow<Boolean> = _isOffline

    override suspend fun getDashboard(): Result<Dashboard> = safeApiCall {
        api.getDashboard().let { dto ->
            Dashboard(
                totalSatellites = dto.totalSatellites ?: 0L,
                satellitesByStatut = dto.satellitesByStatut ?: emptyMap(),
                totalStations = dto.totalStations ?: 0L,
                stationsByStatut = dto.stationsByStatut ?: emptyMap(),
                fenetresPlanifiees = dto.fenetresPlanifiees ?: 0L,
                fenetresRealisees = dto.fenetresRealisees ?: 0L,
                totalMissions = dto.totalMissions ?: 0L,
                missionsByStatut = dto.missionsByStatut ?: emptyMap()
            )
        }
    }

    override fun getSatellitesStream(statut: StatutSatellite?): Flow<List<SatelliteSummary>> = flow {
        try {
            val fresh = api.getSatellites(statut?.label)
            satelliteDao.insertAll(fresh.map { it.toEntity() })
            _isOffline.value = false
            _lastSyncTimestamp.value = System.currentTimeMillis()
        } catch (e: Exception) {
            _isOffline.value = true
        }
        val dbFlow = if (statut == null) satelliteDao.observeAll()
                     else satelliteDao.observeByStatut(statut.label)
        dbFlow.map { it.map { e -> e.toSummaryDomain() } }
              .collect { emit(it) }
    }

    override suspend fun getSatelliteById(id: String): Result<Satellite> {
        return try {
            val dto = api.getSatelliteById(id)
            val satellite = Satellite(
                idSatellite = dto.idSatellite ?: id,
                nomSatellite = dto.nomSatellite ?: "",
                statut = StatutSatellite.fromLabel(dto.statutOperationnel ?: ""),
                formatCubesat = FormatCubeSat.fromLabel(dto.formatCubesat ?: ""),
                dateLancement = dto.dateLancement?.parseLocalDate(),
                masse = dto.masse,
                dureeViePrevue = dto.dureeViePrevue,
                capaciteBatterie = dto.capaciteBatterie,
                orbite = dto.orbite?.let { o ->
                    Orbite(
                        idOrbite = o.idOrbite ?: "",
                        typeOrbite = TypeOrbite.fromLabel(o.typeOrbite ?: ""),
                        altitude = o.altitude ?: 0,
                        inclinaison = o.inclinaison ?: 0.0,
                        periodeOrbitale = o.periodeOrbitale,
                        excentricite = o.excentricite,
                        zoneCouverture = o.zoneCouverture
                    )
                }
            )
            satelliteDao.getById(id)?.let { cached ->
                satelliteDao.insert(
                    cached.copy(
                        masse = dto.masse,
                        dureeViePrevue = dto.dureeViePrevue,
                        capaciteBatterie = dto.capaciteBatterie
                    )
                )
            }
            _isOffline.value = false
            Result.success(satellite)
        } catch (e: Exception) {
            val cached = satelliteDao.getById(id)
            if (cached != null) {
                _isOffline.value = true
                Result.success(cached.toFullDomain())
            } else {
                _isOffline.value = true
                Result.failure(e)
            }
        }
    }

    override suspend fun updateStatutSatellite(id: String, statut: StatutSatellite): Result<Satellite> {
        val request = SatelliteStatutRequestDto(statut.label)
        return try {
            val dto = api.updateStatut(id, request)
            satelliteDao.getById(id)?.let { cached ->
                satelliteDao.insert(cached.copy(statutOperationnel = statut.label))
            }
            Result.success(
                Satellite(
                    idSatellite = dto.idSatellite ?: id,
                    nomSatellite = dto.nomSatellite ?: "",
                    statut = StatutSatellite.fromLabel(dto.statutOperationnel ?: ""),
                    formatCubesat = FormatCubeSat.fromLabel(dto.formatCubesat ?: ""),
                    dateLancement = dto.dateLancement?.parseLocalDate(),
                    masse = dto.masse,
                    dureeViePrevue = dto.dureeViePrevue,
                    capaciteBatterie = dto.capaciteBatterie
                )
            )
        } catch (e: Exception) {
            satelliteDao.getById(id)?.let { cached ->
                satelliteDao.insert(cached.copy(statutOperationnel = statut.label))
            }
            enqueuePendingOperation(
                type = PendingOperationType.UPDATE_STATUT,
                payload = mapOf("id" to id, "statut" to statut.label)
            )
            Result.failure(OfflineException("Mise à jour sauvegardée localement — sera synchronisée au retour du réseau"))
        }
    }

    override suspend fun signalerAnomalie(id: String, description: String): Result<Unit> {
        return try {
            api.signalerAnomalie(id, SatelliteAnomalieRequestDto(description))
            Result.success(Unit)
        } catch (e: Exception) {
            enqueuePendingOperation(
                type = PendingOperationType.SIGNALER_ANOMALIE,
                payload = mapOf("id" to id, "description" to description)
            )
            Result.failure(OfflineException("Anomalie sauvegardée — sera envoyée au retour du réseau"))
        }
    }

    override suspend fun getInstruments(idSatellite: String): Result<List<Embarquement>> {
        return try {
            val list = api.getInstruments(idSatellite).map { dto ->
                Embarquement(
                    refInstrument = dto.refInstrument ?: "",
                    typeInstrument = dto.typeInstrument ?: "",
                    modele = dto.modele ?: "",
                    dateIntegration = dto.dateIntegration?.parseLocalDate(),
                    etatFonctionnement = dto.etatFonctionnement ?: "",
                    commentaire = dto.commentaire
                )
            }
            embarquementDao.deleteByIdSatellite(idSatellite)
            embarquementDao.insertAll(list.map { it.toEntity(idSatellite) })
            Result.success(list)
        } catch (e: Exception) {
            val cached = embarquementDao.getByIdSatellite(idSatellite)
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
            else Result.success(emptyList())
        }
    }

    override suspend fun getMissions(idSatellite: String): Result<List<Participation>> {
        return try {
            val list = api.getMissions(idSatellite).map { dto ->
                Participation(
                    idSatellite = dto.idSatellite ?: "",
                    nomSatellite = dto.nomSatellite ?: "",
                    idMission = dto.idMission ?: "",
                    nomMission = dto.nomMission ?: "",
                    roleSatellite = dto.roleSatellite ?: "",
                    commentaire = dto.commentaire
                )
            }
            participationDao.deleteByIdSatellite(idSatellite)
            participationDao.insertAll(list.map { it.toEntity() })
            Result.success(list)
        } catch (e: Exception) {
            val cached = participationDao.getByIdSatellite(idSatellite)
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
            else Result.success(emptyList())
        }
    }

    override suspend fun getHistoriqueStatut(idSatellite: String): Result<List<HistoriqueStatut>> {
        return try {
            val list = api.getHistoriqueStatut(idSatellite).map { dto ->
                HistoriqueStatut(
                    idHistorique = dto.idHistorique ?: 0L,
                    statut = dto.statut ?: "",
                    timestamp = dto.timestamp?.parseLocalDateTime() ?: LocalDateTime.now()
                )
            }
            historiqueStatutDao.deleteByIdSatellite(idSatellite)
            historiqueStatutDao.insertAll(list.map { it.toEntity(idSatellite) })
            Result.success(list)
        } catch (e: Exception) {
            val cached = historiqueStatutDao.getByIdSatellite(idSatellite)
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
            else Result.success(emptyList())
        }
    }

    override suspend fun getFenetresBySatellite(idSatellite: String): Result<List<FenetreCom>> {
        return try {
            val list = api.getFenetresBySatellite(idSatellite).map { it.toDomain() }
            fenetreDao.insertAll(list.map { it.toEntity() })
            Result.success(list)
        } catch (e: Exception) {
            val cached = fenetreDao.getByIdSatellite(idSatellite)
            Result.success(cached.map { it.toDomain() })
        }
    }

    override fun getFenetresStream(
        statut: String?,
        codeStation: String?,
        idSatellite: String?
    ): Flow<List<FenetreCom>> = flow {
        try {
            val fresh = api.getFenetres(statut, codeStation, idSatellite)
            fenetreDao.insertAll(fresh.map { it.toEntity() })
            _isOffline.value = false
        } catch (e: Exception) {
            _isOffline.value = true
        }
        val dbFlow = when {
            codeStation != null  -> fenetreDao.observeByStation(codeStation)
            idSatellite != null  -> fenetreDao.observeBySatellite(idSatellite)
            statut != null       -> fenetreDao.observeByStatut(statut)
            else                 -> fenetreDao.observeAll()
        }
        dbFlow.map { it.map { e -> e.toDomain() } }.collect { emit(it) }
    }

    override suspend fun getFenetreById(id: Long): Result<FenetreCom> =
        safeApiCall { api.getFenetreById(id).toDomain() }

    override suspend fun createFenetre(request: FenetreComCreateRequest): Result<FenetreCom> {
        val dto = FenetreComCreateRequestDto(
            idSatellite = request.idSatellite,
            codeStation = request.codeStation,
            datetimeDebut = request.datetimeDebut.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            duree = request.duree,
            elevationMax = request.elevationMax
        )
        return try {
            val response = api.createFenetre(dto)
            fenetreDao.insert(response.toEntity())
            Result.success(response.toDomain())
        } catch (e: Exception) {
            val localId = -System.currentTimeMillis()
            val localFenetre = FenetreEntity(
                idFenetre = localId,
                datetimeDebut = dto.datetimeDebut,
                duree = dto.duree,
                elevationMax = dto.elevationMax,
                statut = StatutFenetre.PLANIFIEE.label,
                idSatellite = dto.idSatellite,
                codeStation = dto.codeStation
            )
            fenetreDao.insert(localFenetre)
            enqueuePendingOperation(
                type = PendingOperationType.CREATE_FENETRE,
                payload = mapOf(
                    "localId" to localId.toString(),
                    "idSatellite" to dto.idSatellite,
                    "codeStation" to dto.codeStation,
                    "datetimeDebut" to dto.datetimeDebut,
                    "duree" to dto.duree.toString(),
                    "elevationMax" to dto.elevationMax.toString()
                )
            )
            Result.failure(OfflineException("Fenêtre sauvegardée localement — sera créée au retour du réseau"))
        }
    }

    override suspend fun realiserFenetre(id: Long, volumeDonnees: Double): Result<FenetreCom> {
        return try {
            val response = api.realiserFenetre(id, FenetreComRealiserRequestDto(volumeDonnees))
            fenetreDao.insert(response.toEntity())
            Result.success(response.toDomain())
        } catch (e: Exception) {
            enqueuePendingOperation(
                type = PendingOperationType.REALISER_FENETRE,
                payload = mapOf("id" to id.toString(), "volumeDonnees" to volumeDonnees.toString())
            )
            Result.failure(OfflineException("Clôture sauvegardée — sera synchronisée au retour du réseau"))
        }
    }

    override suspend fun deleteFenetre(id: Long): Result<Unit> {
        return try {
            api.deleteFenetre(id)
            fenetreDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            fenetreDao.deleteById(id)
            enqueuePendingOperation(
                type = PendingOperationType.DELETE_FENETRE,
                payload = mapOf("id" to id.toString())
            )
            Result.failure(OfflineException("Suppression effectuée localement — sera synchronisée au retour du réseau"))
        }
    }

    override fun getStationsStream(): Flow<List<StationSol>> = flow {
        try {
            val fresh = api.getStations()
            stationDao.insertAll(fresh.map { it.toEntity() })
            _isOffline.value = false
            _lastSyncTimestamp.value = System.currentTimeMillis()
        } catch (e: Exception) {
            _isOffline.value = true
        }
        stationDao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .collect { emit(it) }
    }

    override suspend fun getStations(statut: String?): Result<List<StationSol>> =
        safeApiCall {
            val list = api.getStations(statut).map { dto ->
                StationSol(
                    codeStation = dto.codeStation ?: "",
                    nomStation = dto.nomStation ?: "",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    diametreAntenne = dto.diametreAntenne,
                    bandeFrequence = dto.bandeFrequence,
                    debitMax = dto.debitMax,
                    statut = StatutStation.fromLabel(dto.statut ?: "")
                )
            }
            stationDao.insertAll(list.map { it.toEntity() })
            list
        }

    override suspend fun getStationByCode(codeStation: String): Result<StationSol> =
        safeApiCall {
            api.getStationByCode(codeStation).let { dto ->
                StationSol(
                    codeStation = dto.codeStation ?: codeStation,
                    nomStation = dto.nomStation ?: "",
                    latitude = dto.latitude ?: 0.0,
                    longitude = dto.longitude ?: 0.0,
                    diametreAntenne = dto.diametreAntenne,
                    bandeFrequence = dto.bandeFrequence,
                    debitMax = dto.debitMax,
                    statut = StatutStation.fromLabel(dto.statut ?: "")
                )
            }
        }

    override suspend fun getFenetresByStation(codeStation: String): Result<List<FenetreCom>> =
        safeApiCall { api.getFenetresByStation(codeStation).map { it.toDomain() } }

    override fun getLastSyncTimestamp(): Flow<Long?> = _lastSyncTimestamp

    override suspend fun hasPendingOperations(): Boolean =
        pendingOperationDao.count() > 0

    override suspend fun syncPendingOperations(): Result<Int> {
        val pending = pendingOperationDao.getAll()
        if (pending.isEmpty()) return Result.success(0)

        var syncedCount = 0
        for (op in pending) {
            try {
                replayOperation(op)
                pendingOperationDao.deleteById(op.id)
                syncedCount++
            } catch (e: Exception) {
                break
            }
        }
        if (syncedCount > 0) _lastSyncTimestamp.value = System.currentTimeMillis()
        return Result.success(syncedCount)
    }

    override suspend fun forceRefresh(): Result<Unit> = safeApiCall {
        syncPendingOperations()

        val satellites = api.getSatellites()
        satelliteDao.clearAll()
        satelliteDao.insertAll(satellites.map { it.toEntity() })

        val fenetres = api.getFenetres()
        fenetreDao.clearAll()
        fenetreDao.insertAll(fenetres.map { it.toEntity() })

        val stations = api.getStations()
        stationDao.clearAll()
        stationDao.insertAll(stations.map { it.toEntity() })

        _isOffline.value = false
        _lastSyncTimestamp.value = System.currentTimeMillis()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun replayOperation(op: PendingOperationEntity) {
        val payload = gson.fromJson(op.payload, Map::class.java) as Map<String, String>
        when (op.type) {
            PendingOperationType.CREATE_FENETRE -> {
                val response = api.createFenetre(
                    FenetreComCreateRequestDto(
                        idSatellite = payload["idSatellite"] ?: "",
                        codeStation = payload["codeStation"] ?: "",
                        datetimeDebut = payload["datetimeDebut"] ?: "",
                        duree = payload["duree"]?.toInt() ?: 0,
                        elevationMax = payload["elevationMax"]?.toDouble() ?: 0.0
                    )
                )
                val localId = payload["localId"]?.toLong()
                localId?.let { fenetreDao.deleteById(it) }
                fenetreDao.insert(response.toEntity())
            }
            PendingOperationType.DELETE_FENETRE -> {
                api.deleteFenetre(payload["id"]?.toLong() ?: return)
            }
            PendingOperationType.REALISER_FENETRE -> {
                val response = api.realiserFenetre(
                    payload["id"]?.toLong() ?: return,
                    FenetreComRealiserRequestDto(payload["volumeDonnees"]?.toDouble() ?: 0.0)
                )
                fenetreDao.insert(response.toEntity())
            }
            PendingOperationType.SIGNALER_ANOMALIE -> {
                api.signalerAnomalie(
                    payload["id"] ?: return,
                    SatelliteAnomalieRequestDto(payload["description"] ?: "")
                )
            }
            PendingOperationType.UPDATE_STATUT -> {
                api.updateStatut(
                    payload["id"] ?: return,
                    SatelliteStatutRequestDto(payload["statut"] ?: "")
                )
            }
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
        runCatching { block() }.also { result ->
            result.onSuccess { _isOffline.value = false }
            result.onFailure { _isOffline.value = true }
        }

    private suspend fun enqueuePendingOperation(type: String, payload: Map<String, String>) {
        pendingOperationDao.insert(
            PendingOperationEntity(
                type = type,
                payload = gson.toJson(payload)
            )
        )
    }

    private fun StationSolDto.toEntity() = StationEntity(
        codeStation = codeStation ?: "",
        nomStation = nomStation ?: "",
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        diametreAntenne = diametreAntenne,
        bandeFrequence = bandeFrequence,
        debitMax = debitMax,
        statut = statut ?: ""
    )

    private fun StationEntity.toDomain() = StationSol(
        codeStation = codeStation,
        nomStation = nomStation,
        latitude = latitude,
        longitude = longitude,
        diametreAntenne = diametreAntenne,
        bandeFrequence = bandeFrequence,
        debitMax = debitMax,
        statut = StatutStation.fromLabel(statut)
    )

    private fun StationSol.toEntity() = StationEntity(
        codeStation = codeStation,
        nomStation = nomStation,
        latitude = latitude,
        longitude = longitude,
        diametreAntenne = diametreAntenne,
        bandeFrequence = bandeFrequence,
        debitMax = debitMax,
        statut = statut.label
    )

    private fun SatelliteSummaryDto.toEntity() = SatelliteEntity(
        idSatellite = idSatellite ?: "",
        nomSatellite = nomSatellite ?: "",
        statutOperationnel = statutOperationnel ?: "",
        formatCubesat = formatCubesat ?: "",
        dateLancement = dateLancement,
        idOrbite = orbite?.idOrbite,
        typeOrbite = orbite?.typeOrbite,
        altitude = orbite?.altitude,
        inclinaison = orbite?.inclinaison,
        periodeOrbitale = orbite?.periodeOrbitale,
        excentricite = orbite?.excentricite,
        zoneCouverture = orbite?.zoneCouverture
    )

    private fun FenetreComDto.toEntity() = FenetreEntity(
        idFenetre = idFenetre ?: 0L,
        datetimeDebut = datetimeDebut ?: "",
        duree = duree ?: 0,
        elevationMax = elevationMax ?: 0.0,
        volumeDonnees = volumeDonnees,
        statut = statut ?: "",
        idSatellite = idSatellite ?: "",
        nomSatellite = nomSatellite,
        codeStation = codeStation ?: "",
        nomStation = nomStation
    )

    private fun FenetreComDto.toDomain() = FenetreCom(
        idFenetre = idFenetre ?: 0L,
        datetimeDebut = datetimeDebut?.parseLocalDateTime() ?: LocalDateTime.now(),
        duree = duree ?: 0,
        elevationMax = elevationMax ?: 0.0,
        volumeDonnees = volumeDonnees,
        statut = StatutFenetre.fromLabel(statut ?: ""),
        idSatellite = idSatellite ?: "",
        nomSatellite = nomSatellite,
        codeStation = codeStation ?: "",
        nomStation = nomStation
    )

    private fun Embarquement.toEntity(idSatellite: String) = EmbarquementEntity(
        idSatellite = idSatellite,
        refInstrument = refInstrument,
        typeInstrument = typeInstrument,
        modele = modele,
        dateIntegration = dateIntegration?.toString(),
        etatFonctionnement = etatFonctionnement,
        commentaire = commentaire
    )

    private fun Participation.toEntity() = ParticipationEntity(
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        idMission = idMission,
        nomMission = nomMission,
        roleSatellite = roleSatellite,
        commentaire = commentaire
    )

    private fun HistoriqueStatut.toEntity(idSatellite: String) = HistoriqueStatutEntity(
        idHistorique = idHistorique,
        idSatellite = idSatellite,
        statut = statut,
        timestamp = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )

    private fun FenetreCom.toEntity() = FenetreEntity(
        idFenetre = idFenetre,
        datetimeDebut = datetimeDebut.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        duree = duree,
        elevationMax = elevationMax,
        volumeDonnees = volumeDonnees,
        statut = statut.label,
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        codeStation = codeStation,
        nomStation = nomStation
    )

    private fun SatelliteEntity.toFullDomain() = Satellite(
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        statut = StatutSatellite.fromLabel(statutOperationnel),
        formatCubesat = FormatCubeSat.fromLabel(formatCubesat),
        dateLancement = dateLancement?.parseLocalDate(),
        masse = masse,
        dureeViePrevue = dureeViePrevue,
        capaciteBatterie = capaciteBatterie,
        orbite = if (idOrbite != null && typeOrbite != null && altitude != null && inclinaison != null) {
            Orbite(
                idOrbite = idOrbite,
                typeOrbite = TypeOrbite.fromLabel(typeOrbite),
                altitude = altitude,
                inclinaison = inclinaison,
                periodeOrbitale = periodeOrbitale,
                excentricite = excentricite,
                zoneCouverture = zoneCouverture
            )
        } else null
    )

    private fun EmbarquementEntity.toDomain() = Embarquement(
        refInstrument = refInstrument,
        typeInstrument = typeInstrument,
        modele = modele,
        dateIntegration = dateIntegration?.parseLocalDate(),
        etatFonctionnement = etatFonctionnement,
        commentaire = commentaire
    )

    private fun ParticipationEntity.toDomain() = Participation(
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        idMission = idMission,
        nomMission = nomMission,
        roleSatellite = roleSatellite,
        commentaire = commentaire
    )

    private fun HistoriqueStatutEntity.toDomain() = HistoriqueStatut(
        idHistorique = idHistorique,
        statut = statut,
        timestamp = timestamp.parseLocalDateTime() ?: LocalDateTime.now()
    )

    private fun SatelliteEntity.toSummaryDomain() = SatelliteSummary(
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        statut = StatutSatellite.fromLabel(statutOperationnel),
        formatCubesat = FormatCubeSat.fromLabel(formatCubesat),
        dateLancement = dateLancement?.parseLocalDate(),
        orbite = if (idOrbite != null && typeOrbite != null && altitude != null && inclinaison != null) {
            Orbite(
                idOrbite = idOrbite,
                typeOrbite = TypeOrbite.fromLabel(typeOrbite),
                altitude = altitude,
                inclinaison = inclinaison,
                periodeOrbitale = periodeOrbitale,
                excentricite = excentricite,
                zoneCouverture = zoneCouverture
            )
        } else null
    )

    private fun FenetreEntity.toDomain() = FenetreCom(
        idFenetre = idFenetre,
        datetimeDebut = datetimeDebut.parseLocalDateTime() ?: LocalDateTime.now(),
        duree = duree,
        elevationMax = elevationMax,
        volumeDonnees = volumeDonnees,
        statut = StatutFenetre.fromLabel(statut),
        idSatellite = idSatellite,
        nomSatellite = nomSatellite,
        codeStation = codeStation,
        nomStation = nomStation
    )

    override fun getFavoriteIds(): Flow<Set<String>> = favoritesDataStore.favoriteIds

    override suspend fun toggleFavorite(id: String) = favoritesDataStore.toggleFavorite(id)
}

class OfflineException(message: String) : Exception(message)

private fun String.parseLocalDate(): LocalDate? = try {
    LocalDate.parse(this)
} catch (e: DateTimeParseException) { null }

private fun String.parseLocalDateTime(): LocalDateTime? = try {
    val normalized = this.replace("Z", "").take(19)
    LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
} catch (e: DateTimeParseException) { null }