package fr.vlegall.nanoorbitapplication.domain.model

enum class StatutSatellite(val label: String) {
    OPERATIONNEL("Opérationnel"),
    EN_VEILLE("En veille"),
    DEFAILLANT("Défaillant"),
    DESORBITE("Désorbité");

    companion object {
        fun fromLabel(label: String): StatutSatellite =
            entries.firstOrNull { it.label == label } ?: DEFAILLANT
    }
}

enum class FormatCubeSat(val label: String) {
    U1("1U"),
    U3("3U"),
    U6("6U"),
    U12("12U");

    companion object {
        fun fromLabel(label: String): FormatCubeSat =
            entries.firstOrNull { it.label == label } ?: U3
    }
}

enum class StatutFenetre(val label: String) {
    PLANIFIEE("Planifiée"),
    REALISEE("Réalisée"),
    ANNULEE("Annulée");

    companion object {
        fun fromLabel(label: String): StatutFenetre =
            entries.firstOrNull { it.label == label } ?: PLANIFIEE
    }
}

enum class TypeOrbite(val label: String) {
    SSO("SSO"),
    LEO("LEO"),
    MEO("MEO"),
    GEO("GEO");

    companion object {
        fun fromLabel(label: String): TypeOrbite =
            entries.firstOrNull { it.label == label } ?: LEO
    }
}

enum class StatutStation(val label: String) {
    ACTIVE("Active"),
    MAINTENANCE("Maintenance"),
    INACTIVE("Inactive");

    companion object {
        fun fromLabel(label: String): StatutStation =
            entries.firstOrNull { it.label == label } ?: ACTIVE
    }
}