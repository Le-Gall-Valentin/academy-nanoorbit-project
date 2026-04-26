package fr.vlegall.nanoorbitapplication.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import fr.vlegall.nanoorbitapplication.domain.repository.NanoOrbitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    private val repository: NanoOrbitRepository
) : ViewModel() {

    val stations: StateFlow<List<StationSol>> = repository.getStationsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isOffline: StateFlow<Boolean> = repository.isOffline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    fun onUserLocationUpdated(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
    }

    fun dismissError() { _errorMessage.value = null }

    companion object {
        fun provideFactory(app: NanoOrbitApplication): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { MapViewModel(app.container.repository) }
            }
    }
}