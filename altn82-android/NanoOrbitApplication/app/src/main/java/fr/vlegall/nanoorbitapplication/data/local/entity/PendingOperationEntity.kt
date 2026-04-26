package fr.vlegall.nanoorbitapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)

object PendingOperationType {
    const val CREATE_FENETRE      = "CREATE_FENETRE"
    const val DELETE_FENETRE      = "DELETE_FENETRE"
    const val REALISER_FENETRE    = "REALISER_FENETRE"
    const val SIGNALER_ANOMALIE   = "SIGNALER_ANOMALIE"
    const val UPDATE_STATUT       = "UPDATE_STATUT"
}