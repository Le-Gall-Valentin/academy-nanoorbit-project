package fr.vlegall.nanoorbitapplication.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.Embarquement
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.HistoriqueStatut
import fr.vlegall.nanoorbitapplication.domain.model.Participation
import fr.vlegall.nanoorbitapplication.domain.model.Satellite
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.domain.repository.NanoOrbitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: NanoOrbitRepository
) : ViewModel() {

    private val _satellite = MutableStateFlow<Satellite?>(null)
    val satellite: StateFlow<Satellite?> = _satellite.asStateFlow()

    private val _instruments = MutableStateFlow<List<Embarquement>>(emptyList())
    val instruments: StateFlow<List<Embarquement>> = _instruments.asStateFlow()

    private val _missions = MutableStateFlow<List<Participation>>(emptyList())
    val missions: StateFlow<List<Participation>> = _missions.asStateFlow()

    private val _historique = MutableStateFlow<List<HistoriqueStatut>>(emptyList())
    val historique: StateFlow<List<HistoriqueStatut>> = _historique.asStateFlow()

    private val _fenetres = MutableStateFlow<List<FenetreCom>>(emptyList())
    val fenetres: StateFlow<List<FenetreCom>> = _fenetres.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _anomalieSuccess = MutableStateFlow(false)
    val anomalieSuccess: StateFlow<Boolean> = _anomalieSuccess.asStateFlow()

    fun loadSatellite(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            launch {
                repository.getSatelliteById(id)
                    .onSuccess { _satellite.value = it }
                    .onFailure { _errorMessage.value = "Erreur : ${it.message}" }
            }
            launch { repository.getInstruments(id).onSuccess { _instruments.value = it } }
            launch { repository.getMissions(id).onSuccess { _missions.value = it } }
            launch { repository.getHistoriqueStatut(id).onSuccess { _historique.value = it } }
            launch { repository.getFenetresBySatellite(id).onSuccess { _fenetres.value = it } }
            _isLoading.value = false
        }
    }

    fun signalerAnomalie(idSatellite: String, description: String) {
        if (description.length < 5) {
            _errorMessage.value = "La description doit faire au moins 5 caractères"
            return
        }
        viewModelScope.launch {
            repository.signalerAnomalie(idSatellite, description)
                .onSuccess { _anomalieSuccess.value = true }
                .onFailure { _errorMessage.value = "Impossible de signaler l'anomalie : ${it.message}" }
        }
    }

    fun updateStatut(id: String, statut: StatutSatellite) {
        viewModelScope.launch {
            repository.updateStatutSatellite(id, statut)
                .onSuccess { _satellite.value = it }
                .onFailure { _errorMessage.value = "Impossible de mettre à jour le statut : ${it.message}" }
        }
    }

    fun dismissAnomalie() { _anomalieSuccess.value = false }
    fun dismissError() { _errorMessage.value = null }

    companion object {
        fun provideFactory(app: NanoOrbitApplication): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DetailViewModel(app.container.repository) }
            }
    }
}