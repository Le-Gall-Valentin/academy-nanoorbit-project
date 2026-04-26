package fr.vlegall.nanoorbitapplication.domain.model

import java.time.LocalDateTime

data class HistoriqueStatut(
    val idHistorique: Long,
    val statut: String,
    val timestamp: LocalDateTime
)