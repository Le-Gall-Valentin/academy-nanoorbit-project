package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fenetres_com")
data class FenetreEntity(
    @PrimaryKey
    val idFenetre: Long,
    val datetimeDebut: String,
    val duree: Int,
    val elevationMax: Double,
    val volumeDonnees: Double? = null,
    val statut: String,
    val idSatellite: String,
    val nomSatellite: String? = null,
    val codeStation: String,
    val nomStation: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)