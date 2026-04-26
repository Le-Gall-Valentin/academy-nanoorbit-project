package fr.vlegall.nanoorbitapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.vlegall.nanoorbitapplication.data.local.entity.EmbarquementEntity

@Dao
interface EmbarquementDao {

    @Query("SELECT * FROM embarquements WHERE idSatellite = :idSatellite")
    suspend fun getByIdSatellite(idSatellite: String): List<EmbarquementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embarquements: List<EmbarquementEntity>)

    @Query("DELETE FROM embarquements WHERE idSatellite = :idSatellite")
    suspend fun deleteByIdSatellite(idSatellite: String)
}