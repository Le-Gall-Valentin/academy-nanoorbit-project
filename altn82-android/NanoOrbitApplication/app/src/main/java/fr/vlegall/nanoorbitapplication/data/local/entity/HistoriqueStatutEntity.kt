package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historique_statut")
data class HistoriqueStatutEntity(
    @PrimaryKey val idHistorique: Long,
    val idSatellite: String,
    val statut: String,
    val timestamp: String
)