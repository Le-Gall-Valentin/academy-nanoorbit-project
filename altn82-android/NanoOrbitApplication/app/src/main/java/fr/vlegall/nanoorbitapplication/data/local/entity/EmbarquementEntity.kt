package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity

@Entity(tableName = "embarquements", primaryKeys = ["idSatellite", "refInstrument"])
data class EmbarquementEntity(
    val idSatellite: String,
    val refInstrument: String,
    val typeInstrument: String,
    val modele: String,
    val dateIntegration: String? = null,
    val etatFonctionnement: String,
    val commentaire: String? = null
)