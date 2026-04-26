package fr.vlegall.nanoorbitapplication.mock

import fr.vlegall.nanoorbitapplication.domain.model.Embarquement
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.FormatCubeSat
import fr.vlegall.nanoorbitapplication.domain.model.Orbite
import fr.vlegall.nanoorbitapplication.domain.model.Participation
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StatutFenetre
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.domain.model.StatutStation
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import fr.vlegall.nanoorbitapplication.domain.model.TypeOrbite
import java.time.LocalDate
import java.time.LocalDateTime

object MockData {

    val orbite_SSO_500 = Orbite(
        idOrbite = "ORB-001",
        typeOrbite = TypeOrbite.SSO,
        altitude = 500,
        inclinaison = 97.4,
        periodeOrbitale = 94.5,
        excentricite = 0.0012,
        zoneCouverture = "Global"
    )

    val orbite_SSO_600 = Orbite(
        idOrbite = "ORB-002",
        typeOrbite = TypeOrbite.SSO,
        altitude = 600,
        inclinaison = 98.0,
        periodeOrbitale = 96.7,
        excentricite = 0.0008,
        zoneCouverture = "Polaire"
    )

    val orbite_LEO_450 = Orbite(
        idOrbite = "ORB-003",
        typeOrbite = TypeOrbite.LEO,
        altitude = 450,
        inclinaison = 51.6,
        periodeOrbitale = 92.8,
        excentricite = 0.0005,
        zoneCouverture = "Équatorial"
    )

    val satellites = listOf(
        SatelliteSummary(
            idSatellite = "SAT-001",
            nomSatellite = "NanoOrbit-Alpha",
            statut = StatutSatellite.OPERATIONNEL,
            formatCubesat = FormatCubeSat.U6,
            dateLancement = LocalDate.of(2022, 3, 15),
            orbite = orbite_SSO_500
        ),
        SatelliteSummary(
            idSatellite = "SAT-002",
            nomSatellite = "NanoOrbit-Beta",
            statut = StatutSatellite.OPERATIONNEL,
            formatCubesat = FormatCubeSat.U3,
            dateLancement = LocalDate.of(2022, 6, 20),
            orbite = orbite_SSO_600
        ),
        SatelliteSummary(
            idSatellite = "SAT-003",
            nomSatellite = "NanoOrbit-Gamma",
            statut = StatutSatellite.EN_VEILLE,
            formatCubesat = FormatCubeSat.U6,
            dateLancement = LocalDate.of(2023, 1, 10),
            orbite = orbite_LEO_450
        ),
        SatelliteSummary(
            idSatellite = "SAT-004",
            nomSatellite = "NanoOrbit-Delta",
            statut = StatutSatellite.DEFAILLANT,
            formatCubesat = FormatCubeSat.U12,
            dateLancement = LocalDate.of(2023, 8, 5),
            orbite = orbite_SSO_500
        ),
        SatelliteSummary(
            idSatellite = "SAT-005",
            nomSatellite = "NanoOrbit-Epsilon",
            statut = StatutSatellite.DESORBITE,
            formatCubesat = FormatCubeSat.U3,
            dateLancement = LocalDate.of(2021, 11, 30),
            orbite = orbite_LEO_450
        )
    )

    val stations = listOf(
        StationSol(
            codeStation = "GS-PAR",
            nomStation = "Paris — Centre de contrôle Europe",
            latitude = 48.8566,
            longitude = 2.3522,
            diametreAntenne = 5.4,
            bandeFrequence = "S",
            debitMax = 150.0,
            statut = StatutStation.ACTIVE
        ),
        StationSol(
            codeStation = "GS-SGP",
            nomStation = "Singapour — Centre Asie-Pacifique",
            latitude = 1.3521,
            longitude = 103.8198,
            diametreAntenne = 7.2,
            bandeFrequence = "X",
            debitMax = 250.0,
            statut = StatutStation.ACTIVE
        ),
        StationSol(
            codeStation = "GS-HOU",
            nomStation = "Houston — Centre Amériques",
            latitude = 29.7604,
            longitude = -95.3698,
            diametreAntenne = 6.0,
            bandeFrequence = "S",
            debitMax = 200.0,
            statut = StatutStation.MAINTENANCE
        )
    )

    val instruments = listOf(
        Embarquement(
            refInstrument = "CAM-VIS-01",
            typeInstrument = "Caméra visible",
            modele = "Hyperion EO-1",
            dateIntegration = LocalDate.of(2022, 1, 10),
            etatFonctionnement = "Nominal",
            commentaire = "Résolution 5m — surveillance déforestation"
        ),
        Embarquement(
            refInstrument = "SAR-L-01",
            typeInstrument = "Radar SAR",
            modele = "MiniSAR L-Band",
            dateIntegration = LocalDate.of(2022, 2, 15),
            etatFonctionnement = "Nominal",
            commentaire = "Pénétration nuages — surveillance côtes"
        ),
        Embarquement(
            refInstrument = "AIS-01",
            typeInstrument = "Récepteur AIS",
            modele = "exactEarth AIS",
            dateIntegration = LocalDate.of(2023, 1, 5),
            etatFonctionnement = "Dégradé",
            commentaire = "Suivi trafic maritime — puissance réduite 30%"
        ),
        Embarquement(
            refInstrument = "THERM-IR-01",
            typeInstrument = "Capteur infrarouge thermique",
            modele = "FLIR Boson+",
            dateIntegration = LocalDate.of(2022, 6, 1),
            etatFonctionnement = "Nominal",
            commentaire = "Détection points chauds / incendies"
        )
    )

    val fenetres = listOf(
        FenetreCom(
            idFenetre = 1L,
            datetimeDebut = LocalDateTime.now().minusHours(48),
            duree = 480,
            elevationMax = 75.3,
            volumeDonnees = 1250.5,
            statut = StatutFenetre.REALISEE,
            idSatellite = "SAT-001",
            nomSatellite = "NanoOrbit-Alpha",
            codeStation = "GS-PAR",
            nomStation = "Paris — Centre de contrôle Europe"
        ),
        FenetreCom(
            idFenetre = 2L,
            datetimeDebut = LocalDateTime.now().minusHours(24),
            duree = 360,
            elevationMax = 62.1,
            volumeDonnees = 890.0,
            statut = StatutFenetre.REALISEE,
            idSatellite = "SAT-002",
            nomSatellite = "NanoOrbit-Beta",
            codeStation = "GS-SGP",
            nomStation = "Singapour — Centre Asie-Pacifique"
        ),
        FenetreCom(
            idFenetre = 3L,
            datetimeDebut = LocalDateTime.now().minusHours(6),
            duree = 600,
            elevationMax = 88.0,
            volumeDonnees = 2100.0,
            statut = StatutFenetre.REALISEE,
            idSatellite = "SAT-001",
            nomSatellite = "NanoOrbit-Alpha",
            codeStation = "GS-SGP",
            nomStation = "Singapour — Centre Asie-Pacifique"
        ),
        FenetreCom(
            idFenetre = 4L,
            datetimeDebut = LocalDateTime.now().plusHours(2),
            duree = 420,
            elevationMax = 71.5,
            volumeDonnees = null,
            statut = StatutFenetre.PLANIFIEE,
            idSatellite = "SAT-002",
            nomSatellite = "NanoOrbit-Beta",
            codeStation = "GS-PAR",
            nomStation = "Paris — Centre de contrôle Europe"
        ),
        FenetreCom(
            idFenetre = 5L,
            datetimeDebut = LocalDateTime.now().plusHours(8),
            duree = 300,
            elevationMax = 55.2,
            volumeDonnees = null,
            statut = StatutFenetre.PLANIFIEE,
            idSatellite = "SAT-003",
            nomSatellite = "NanoOrbit-Gamma",
            codeStation = "GS-HOU",
            nomStation = "Houston — Centre Amériques"
        )
    )

    val participations = listOf(
        Participation(
            idSatellite = "SAT-001",
            nomSatellite = "NanoOrbit-Alpha",
            idMission = "MSN-001",
            nomMission = "CôteSurvey 2024",
            roleSatellite = "Imageur principal",
            commentaire = "Couverture Atlantique Nord"
        ),
        Participation(
            idSatellite = "SAT-002",
            nomSatellite = "NanoOrbit-Beta",
            idMission = "MSN-001",
            nomMission = "CôteSurvey 2024",
            roleSatellite = "Imageur secondaire",
            commentaire = null
        )
    )
}