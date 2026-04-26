package fr.vlegall.nanoorbitapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.vlegall.nanoorbitapplication.data.local.entity.FenetreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FenetreDao {

    @Query("SELECT * FROM fenetres_com ORDER BY datetimeDebut ASC")
    fun observeAll(): Flow<List<FenetreEntity>>

    @Query("SELECT * FROM fenetres_com WHERE statut = :statut ORDER BY datetimeDebut ASC")
    fun observeByStatut(statut: String): Flow<List<FenetreEntity>>

    @Query("SELECT * FROM fenetres_com WHERE codeStation = :codeStation ORDER BY datetimeDebut ASC")
    fun observeByStation(codeStation: String): Flow<List<FenetreEntity>>

    @Query("SELECT * FROM fenetres_com WHERE idSatellite = :idSatellite ORDER BY datetimeDebut ASC")
    fun observeBySatellite(idSatellite: String): Flow<List<FenetreEntity>>

    @Query("SELECT * FROM fenetres_com WHERE idSatellite = :idSatellite ORDER BY datetimeDebut ASC")
    suspend fun getByIdSatellite(idSatellite: String): List<FenetreEntity>

    @Query("SELECT * FROM fenetres_com WHERE idFenetre = :id LIMIT 1")
    suspend fun getById(id: Long): FenetreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fenetres: List<FenetreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fenetre: FenetreEntity)

    @Query("DELETE FROM fenetres_com WHERE idFenetre = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fenetres_com")
    suspend fun clearAll()

    @Query("SELECT MAX(lastUpdated) FROM fenetres_com")
    suspend fun getLastUpdatedTimestamp(): Long?
}