package fr.vlegall.nanoorbitapplication.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.Dashboard
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.domain.repository.NanoOrbitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: NanoOrbitRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val selectedStatut = MutableStateFlow<StatutSatellite?>(null)

    private val _dashboard = MutableStateFlow<Dashboard?>(null)
    val dashboard: StateFlow<Dashboard?> = _dashboard.asStateFlow()

    val lastSyncTimestamp: StateFlow<Long?> = repository.getLastSyncTimestamp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val favoriteIds: StateFlow<Set<String>> = repository.getFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val showOnlyFavorites = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _satellites: StateFlow<List<SatelliteSummary>> = selectedStatut
        .flatMapLatest { statut -> repository.getSatellitesStream(statut) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val satellites: StateFlow<List<SatelliteSummary>> = _satellites

    val filteredSatellites: StateFlow<List<SatelliteSummary>> = combine(
        _satellites, searchQuery, favoriteIds, showOnlyFavorites
    ) { list, query, favIds, onlyFav ->
        var result = if (query.isBlank()) list
        else list.filter { sat ->
            sat.nomSatellite.contains(query, ignoreCase = true) ||
            sat.idSatellite.contains(query, ignoreCase = true) ||
            (sat.orbite?.typeOrbite?.label?.contains(query, ignoreCase = true) == true)
        }
        if (onlyFav) result = result.filter { it.idSatellite in favIds }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadSatellites()
        loadDashboard()
    }

    fun loadSatellites() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.forceRefresh()
                .onFailure { e -> _errorMessage.value = "Erreur de chargement : ${e.message}" }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) { searchQuery.value = query }
    fun onStatutFilterChange(statut: StatutSatellite?) { selectedStatut.value = statut }
    fun refreshSatellites() { loadSatellites() }
    fun dismissError() { _errorMessage.value = null }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun onShowOnlyFavoritesChange(show: Boolean) { showOnlyFavorites.value = show }

    private fun loadDashboard() {
        viewModelScope.launch {
            repository.getDashboard()
                .onSuccess { _dashboard.value = it }
        }
    }

    companion object {
        fun provideFactory(app: NanoOrbitApplication): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DashboardViewModel(app.container.repository) }
            }
    }
}