package fr.vlegall.nanoorbitapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.vlegall.nanoorbitapplication.data.local.entity.ParticipationEntity

@Dao
interface ParticipationDao {

    @Query("SELECT * FROM participations WHERE idSatellite = :idSatellite")
    suspend fun getByIdSatellite(idSatellite: String): List<ParticipationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participations: List<ParticipationEntity>)

    @Query("DELETE FROM participations WHERE idSatellite = :idSatellite")
    suspend fun deleteByIdSatellite(idSatellite: String)
}