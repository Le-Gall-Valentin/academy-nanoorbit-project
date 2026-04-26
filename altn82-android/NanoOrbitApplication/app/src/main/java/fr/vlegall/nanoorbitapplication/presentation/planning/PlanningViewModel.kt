package fr.vlegall.nanoorbitapplication.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.data.repository.OfflineException
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.FenetreComCreateRequest
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StatutFenetre
import fr.vlegall.nanoorbitapplication.domain.model.StatutStation
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import fr.vlegall.nanoorbitapplication.domain.repository.NanoOrbitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanningViewModel(
    private val repository: NanoOrbitRepository
) : ViewModel() {

    val isOffline: StateFlow<Boolean> = repository.isOffline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastSyncTimestamp: StateFlow<Long?> = repository.getLastSyncTimestamp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val satellites: StateFlow<List<SatelliteSummary>> = repository.getSatellitesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stations: StateFlow<List<StationSol>> = repository.getStationsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedStation = MutableStateFlow<String?>(null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createSuccess = MutableStateFlow(false)
    val createSuccess: StateFlow<Boolean> = _createSuccess.asStateFlow()

    private val _pendingSync = MutableStateFlow(false)
    val pendingSync: StateFlow<Boolean> = _pendingSync.asStateFlow()

    val fenetres: StateFlow<List<FenetreCom>> = combine(
        repository.getFenetresStream(),
        selectedStation
    ) { list, station ->
        val sorted = list.sortedBy { it.datetimeDebut }
        if (station != null) sorted.filter { it.codeStation == station } else sorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshFenetres()
    }

    fun onStationSelected(codeStation: String?) { selectedStation.value = codeStation }

    fun refreshFenetres() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.forceRefresh()
                .onFailure { _errorMessage.value = "Actualisation impossible : ${it.message}" }
            _isLoading.value = false
        }
    }

    fun createFenetre(request: FenetreComCreateRequest) {
        if (request.duree < FenetreCom.DUREE_MIN_SECONDES || request.duree > FenetreCom.DUREE_MAX_SECONDES) {
            _errorMessage.value =
                "La durée doit être comprise entre ${FenetreCom.DUREE_MIN_SECONDES} et ${FenetreCom.DUREE_MAX_SECONDES} secondes (RG-F04)"
            return
        }

        val station = stations.value.find { it.codeStation == request.codeStation }
        if (station?.statut == StatutStation.MAINTENANCE) {
            _errorMessage.value =
                "La station ${station.nomStation} est en maintenance — impossible de planifier (RG-G03)"
            return
        }

        val satellite = satellites.value.find { it.idSatellite == request.idSatellite }
        if (satellite == null) {
            _errorMessage.value = "Satellite introuvable: ${request.idSatellite}"
            return
        }
        if (!satellite.canScheduleWindow) {
            _errorMessage.value =
                "Le satellite ${satellite.nomSatellite} est désorbité — planification interdite (RG-S06)"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            repository.createFenetre(request)
                .onSuccess { _createSuccess.value = true }
                .onFailure { e ->
                    if (e is OfflineException) {
                        _createSuccess.value = true
                        _pendingSync.value = true
                    } else {
                        _errorMessage.value = "Erreur de planification : ${e.message}"
                    }
                }
            _isLoading.value = false
        }
    }

    fun realiserFenetre(idFenetre: Long, volumeDonnees: Double) {
        if (volumeDonnees < 0) {
            _errorMessage.value = "Le volume de données ne peut pas être négatif"
            return
        }
        viewModelScope.launch {
            repository.realiserFenetre(idFenetre, volumeDonnees)
                .onFailure { _errorMessage.value = "Erreur : ${it.message}" }
        }
    }

    fun deleteFenetre(idFenetre: Long, statut: StatutFenetre) {
        if (statut != StatutFenetre.PLANIFIEE) {
            _errorMessage.value = "Seules les fenêtres Planifiées peuvent être supprimées"
            return
        }
        viewModelScope.launch {
            repository.deleteFenetre(idFenetre)
                .onFailure { _errorMessage.value = "Suppression impossible : ${it.message}" }
        }
    }

    fun dismissError() { _errorMessage.value = null }
    fun dismissSuccess() {
        _createSuccess.value = false
        _pendingSync.value = false
    }

    companion object {
        fun provideFactory(app: NanoOrbitApplication): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PlanningViewModel(app.container.repository) }
            }
    }
}