package fr.vlegall.nanoorbitapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.vlegall.nanoorbitapplication.data.local.entity.SatelliteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SatelliteDao {

    @Query("SELECT * FROM satellites ORDER BY nomSatellite ASC")
    fun observeAll(): Flow<List<SatelliteEntity>>

    @Query("SELECT * FROM satellites WHERE statutOperationnel = :statut ORDER BY nomSatellite ASC")
    fun observeByStatut(statut: String): Flow<List<SatelliteEntity>>

    @Query("SELECT * FROM satellites WHERE idSatellite = :id LIMIT 1")
    suspend fun getById(id: String): SatelliteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(satellites: List<SatelliteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(satellite: SatelliteEntity)

    @Query("DELETE FROM satellites")
    suspend fun clearAll()

    @Query("SELECT MAX(lastUpdated) FROM satellites")
    suspend fun getLastUpdatedTimestamp(): Long?
}