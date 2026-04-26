package fr.vlegall.nanoorbitapplication.di

import android.content.Context
import android.util.Log
import fr.vlegall.nanoorbitapplication.data.local.FavoritesDataStore
import fr.vlegall.nanoorbitapplication.data.local.NanoOrbitDatabase
import fr.vlegall.nanoorbitapplication.data.network.NetworkConnectivityObserver
import fr.vlegall.nanoorbitapplication.data.remote.RetrofitClient
import fr.vlegall.nanoorbitapplication.data.repository.NanoOrbitRepositoryImpl
import fr.vlegall.nanoorbitapplication.domain.repository.NanoOrbitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val database = NanoOrbitDatabase.getInstance(context)
    private val api = RetrofitClient.create()
    private val favoritesDataStore = FavoritesDataStore(context)
    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val networkObserver = NetworkConnectivityObserver(context)

    val repository: NanoOrbitRepository = NanoOrbitRepositoryImpl(
        api = api,
        satelliteDao = database.satelliteDao(),
        fenetreDao = database.fenetreDao(),
        pendingOperationDao = database.pendingOperationDao(),
        stationDao = database.stationDao(),
        embarquementDao = database.embarquementDao(),
        participationDao = database.participationDao(),
        historiqueStatutDao = database.historiqueStatutDao(),
        favoritesDataStore = favoritesDataStore
    )

    init {
        containerScope.launch {
            networkObserver.isConnected.collect { connected ->
                if (connected && repository.hasPendingOperations()) {
                    Log.d("AppContainer", "Réseau rétabli — synchronisation des opérations en attente")
                    val result = repository.syncPendingOperations()
                    result.onSuccess { count ->
                        if (count > 0) Log.i("AppContainer", "$count opération(s) synchronisée(s)")
                    }
                    result.onFailure { e ->
                        Log.w("AppContainer", "Échec de la synchronisation : ${e.message}")
                    }
                }
            }
        }
    }
}