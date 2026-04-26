package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val codeStation: String,
    val nomStation: String,
    val latitude: Double,
    val longitude: Double,
    val diametreAntenne: Double? = null,
    val bandeFrequence: String? = null,
    val debitMax: Double? = null,
    val statut: String,
    val lastUpdated: Long = System.currentTimeMillis()
)