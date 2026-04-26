package fr.vlegall.nanoorbitapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.vlegall.nanoorbitapplication.data.local.entity.HistoriqueStatutEntity

@Dao
interface HistoriqueStatutDao {

    @Query("SELECT * FROM historique_statut WHERE idSatellite = :idSatellite ORDER BY timestamp DESC")
    suspend fun getByIdSatellite(idSatellite: String): List<HistoriqueStatutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(historique: List<HistoriqueStatutEntity>)

    @Query("DELETE FROM historique_statut WHERE idSatellite = :idSatellite")
    suspend fun deleteByIdSatellite(idSatellite: String)
}