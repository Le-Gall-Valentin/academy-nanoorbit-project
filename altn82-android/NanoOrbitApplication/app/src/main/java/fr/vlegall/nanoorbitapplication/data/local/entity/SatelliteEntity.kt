package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "satellites")
data class SatelliteEntity(
    @PrimaryKey
    val idSatellite: String,
    val nomSatellite: String,
    val statutOperationnel: String,
    val formatCubesat: String,
    val dateLancement: String? = null,
    val masse: Double? = null,
    val dureeViePrevue: Int? = null,
    val capaciteBatterie: Double? = null,
    val idOrbite: String? = null,
    val typeOrbite: String? = null,
    val altitude: Int? = null,
    val inclinaison: Double? = null,
    val periodeOrbitale: Double? = null,
    val excentricite: Double? = null,
    val zoneCouverture: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)