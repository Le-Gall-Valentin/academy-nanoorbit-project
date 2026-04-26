package fr.vlegall.nanoorbitapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.vlegall.nanoorbitapplication.data.local.converter.DateConverter
import fr.vlegall.nanoorbitapplication.data.local.dao.EmbarquementDao
import fr.vlegall.nanoorbitapplication.data.local.dao.FenetreDao
import fr.vlegall.nanoorbitapplication.data.local.dao.HistoriqueStatutDao
import fr.vlegall.nanoorbitapplication.data.local.dao.ParticipationDao
import fr.vlegall.nanoorbitapplication.data.local.dao.PendingOperationDao
import fr.vlegall.nanoorbitapplication.data.local.dao.SatelliteDao
import fr.vlegall.nanoorbitapplication.data.local.dao.StationDao
import fr.vlegall.nanoorbitapplication.data.local.entity.EmbarquementEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.FenetreEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.HistoriqueStatutEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.ParticipationEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.PendingOperationEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.SatelliteEntity
import fr.vlegall.nanoorbitapplication.data.local.entity.StationEntity

@Database(
    entities = [
        SatelliteEntity::class,
        FenetreEntity::class,
        PendingOperationEntity::class,
        StationEntity::class,
        EmbarquementEntity::class,
        ParticipationEntity::class,
        HistoriqueStatutEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class NanoOrbitDatabase : RoomDatabase() {

    abstract fun satelliteDao(): SatelliteDao
    abstract fun fenetreDao(): FenetreDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun stationDao(): StationDao
    abstract fun embarquementDao(): EmbarquementDao
    abstract fun participationDao(): ParticipationDao
    abstract fun historiqueStatutDao(): HistoriqueStatutDao

    companion object {
        @Volatile
        private var INSTANCE: NanoOrbitDatabase? = null

        fun getInstance(context: Context): NanoOrbitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NanoOrbitDatabase::class.java,
                    "nanoorbit_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}