package fr.vlegall.nanoorbitapplication.domain.model

import java.time.LocalDate

data class Embarquement(
    val refInstrument: String,
    val typeInstrument: String,
    val modele: String,
    val dateIntegration: LocalDate? = null,
    val etatFonctionnement: String,
    val commentaire: String? = null
)