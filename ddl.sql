create sequence SEQ_HISTORIQUE_STATUT
    nocache
/

create sequence SEQ_SATELLITE
    nocache
/

create sequence SEQ_ORBITE
    nocache
/

create sequence SEQ_CENTRE
    nocache
/

create sequence SEQ_FENETRE
    nocache
/

create table ORBITE
(
    ID_ORBITE        VARCHAR2(20) not null
        constraint PK_ORBITE
            primary key,
    TYPE_ORBITE      VARCHAR2(10) not null
        constraint CK_ORBITE_TYPE
            check (type_orbite IN ('LEO', 'MEO', 'SSO', 'GEO')),
    ALTITUDE         NUMBER(5)    not null
        constraint CK_ORBITE_ALTITUDE
            check (altitude > 0),
    INCLINAISON      NUMBER(5, 2) not null
        constraint CK_ORBITE_INCLINAISON
            check (inclinaison BETWEEN 0 AND 180),
    PERIODE_ORBITALE NUMBER(6, 2) not null
        constraint CK_ORBITE_PERIODE
            check (periode_orbitale > 0),
    EXCENTRICITE     NUMBER(6, 4) not null
        constraint CK_ORBITE_EXCENTRICITE
            check (excentricite >= 0 AND excentricite < 1),
    ZONE_COUVERTURE  VARCHAR2(200),
    constraint UQ_ORBITE_ALT_INC
        unique (ALTITUDE, INCLINAISON)
)
/

create table SATELLITE
(
    ID_SATELLITE      VARCHAR2(20)  not null
        constraint PK_SATELLITE
            primary key,
    NOM_SATELLITE     VARCHAR2(100) not null
        constraint UQ_SATELLITE_NOM
            unique,
    DATE_LANCEMENT    DATE          not null,
    MASSE             NUMBER(5, 2)  not null
        constraint CK_SATELLITE_MASSE
            check (masse > 0),
    FORMAT_CUBESAT    VARCHAR2(5)   not null
        constraint CK_SATELLITE_FORMAT
            check (format_cubesat IN ('1U', '3U', '6U', '12U')),
    STATUT            VARCHAR2(30)  not null
        constraint CK_SATELLITE_STATUT
            check (statut IN ('Opérationnel', 'En veille', 'Défaillant', 'Désorbité')),
    DUREE_VIE_PREVUE  NUMBER(5)     not null
        constraint CK_SATELLITE_DUREE
            check (duree_vie_prevue > 0),
    CAPACITE_BATTERIE NUMBER(6, 1)  not null
        constraint CK_SATELLITE_BATTERIE
            check (capacite_batterie > 0),
    ID_ORBITE         VARCHAR2(20)  not null
        constraint FK_SATELLITE_ORBITE
            references ORBITE
)
/

create index IDX_SATELLITE_ID_ORBITE
    on SATELLITE (ID_ORBITE)
/

create index IDX_SATELLITE_STATUT
    on SATELLITE (STATUT)
/

create index IDX_SATELLITE_STATUT_ORBITE
    on SATELLITE (STATUT, ID_ORBITE)
/

create table INSTRUMENT
(
    REF_INSTRUMENT  VARCHAR2(20)  not null
        constraint PK_INSTRUMENT
            primary key,
    TYPE_INSTRUMENT VARCHAR2(50)  not null,
    MODELE          VARCHAR2(100) not null,
    RESOLUTION      NUMBER(6, 1)
        constraint CK_INSTRUMENT_RESOLUTION
            check (resolution IS NULL OR resolution > 0),
    CONSOMMATION    NUMBER(5, 2)  not null
        constraint CK_INSTRUMENT_CONSOMMATION
            check (consommation > 0),
    MASSE           NUMBER(5, 3)  not null
        constraint CK_INSTRUMENT_MASSE
            check (masse > 0)
)
/

create table CENTRE_CONTROLE
(
    ID_CENTRE      VARCHAR2(20)  not null
        constraint PK_CENTRE_CONTROLE
            primary key,
    NOM_CENTRE     VARCHAR2(100) not null,
    VILLE          VARCHAR2(50)  not null,
    REGION_GEO     VARCHAR2(50)  not null
        constraint CK_CENTRE_REGION
            check (region_geo IN ('Europe', 'Amériques', 'Asie-Pacifique')),
    FUSEAU_HORAIRE VARCHAR2(50)  not null,
    STATUT         VARCHAR2(20)  not null
        constraint CK_CENTRE_STATUT
            check (statut IN ('Actif', 'Inactif'))
)
/

create table STATION_SOL
(
    CODE_STATION     VARCHAR2(20)  not null
        constraint PK_STATION_SOL
            primary key,
    NOM_STATION      VARCHAR2(100) not null,
    LATITUDE         NUMBER(9, 6)  not null
        constraint CK_STATION_LATITUDE
            check (latitude BETWEEN -90 AND 90),
    LONGITUDE        NUMBER(15, 6) not null
        constraint CK_STATION_LONGITUDE
            check (longitude BETWEEN -180 AND 180),
    DIAMETRE_ANTENNE NUMBER(4, 1)  not null
        constraint CK_STATION_DIAMETRE
            check (diametre_antenne > 0),
    BANDE_FREQUENCE  VARCHAR2(10)  not null
        constraint CK_STATION_BANDE
            check (bande_frequence IN ('UHF', 'S', 'X', 'Ka')),
    DEBIT_MAX        NUMBER(6, 1)  not null
        constraint CK_STATION_DEBIT
            check (debit_max > 0),
    STATUT           VARCHAR2(20)  not null
        constraint CK_STATION_STATUT
            check (statut IN ('Active', 'Maintenance', 'Inactive'))
)
/

create index IDX_STATION_STATUT
    on STATION_SOL (STATUT)
/

create table MISSION
(
    ID_MISSION     VARCHAR2(20)  not null
        constraint PK_MISSION
            primary key,
    NOM_MISSION    VARCHAR2(100) not null,
    OBJECTIF       CLOB          not null,
    ZONE_GEO_CIBLE VARCHAR2(200) not null,
    DATE_DEBUT     DATE          not null,
    DATE_FIN       DATE,
    STATUT_MISSION VARCHAR2(20)  not null
        constraint CK_MISSION_STATUT
            check (statut_mission IN ('Active', 'Terminée')),
    constraint CK_MISSION_DATES
        check (date_fin IS NULL OR date_fin >= date_debut)
)
/

create index IDX_MISSION_STATUT
    on MISSION (STATUT_MISSION)
/

create table FENETRE_COM
(
    ID_FENETRE     NUMBER(10)   not null
        constraint PK_FENETRE_COM
            primary key,
    DATETIME_DEBUT TIMESTAMP(6) not null,
    DUREE          NUMBER(4)    not null
        constraint CK_FENETRE_DUREE
            check (duree BETWEEN 1 AND 900),
    ELEVATION_MAX  NUMBER(5, 2) not null
        constraint CK_FENETRE_ELEVATION
            check (elevation_max BETWEEN 0 AND 90),
    VOLUME_DONNEES NUMBER(8, 1)
        constraint CK_FENETRE_VOLUME
            check (volume_donnees IS NULL OR volume_donnees >= 0),
    STATUT         VARCHAR2(20) not null
        constraint CK_FENETRE_STATUT
            check (statut IN ('Planifiée', 'Réalisée')),
    CODE_STATION   VARCHAR2(20) not null
        constraint FK_FENETRE_STATION
            references STATION_SOL,
    ID_SATELLITE   VARCHAR2(20) not null
        constraint FK_FENETRE_SATELLITE
            references SATELLITE
)
/

create index IDX_FENETRE_CODE_STATION
    on FENETRE_COM (CODE_STATION)
/

create index IDX_FENETRE_ID_SATELLITE
    on FENETRE_COM (ID_SATELLITE)
/

create index IDX_FENETRE_DATETIME
    on FENETRE_COM (DATETIME_DEBUT)
/

create index IDX_FENETRE_MOIS
    on FENETRE_COM (TRUNC("DATETIME_DEBUT", 'fmmm'))
/

create table HISTORIQUE_STATUT
(
    ID_HISTORIQUE NUMBER(10)   not null
        constraint PK_HISTORIQUE_STATUT
            primary key,
    STATUT        VARCHAR2(30) not null
        constraint CK_HISTORIQUE_STATUT
            check (statut IN ('Opérationnel', 'En veille', 'Défaillant', 'Désorbité')),
    TIMESTAMP_    TIMESTAMP(6) not null,
    ID_SATELLITE  VARCHAR2(20) not null
        constraint FK_HIST_SATELLITE
            references SATELLITE
)
/

create index IDX_HISTORIQUE_ID_SATELLITE
    on HISTORIQUE_STATUT (ID_SATELLITE)
/

create index IDX_HISTORIQUE_SAT_TS
    on HISTORIQUE_STATUT (ID_SATELLITE, TIMESTAMP_)
/

create table EMBARQUEMENT
(
    ID_SATELLITE        VARCHAR2(20) not null
        constraint FK_EMB_SATELLITE
            references SATELLITE,
    REF_INSTRUMENT      VARCHAR2(20) not null
        constraint FK_EMB_INSTRUMENT
            references INSTRUMENT,
    DATE_INTEGRATION    DATE         not null,
    ETAT_FONCTIONNEMENT VARCHAR2(20) not null
        constraint CK_EMB_ETAT
            check (etat_fonctionnement IN ('Nominal', 'Dégradé', 'Hors service')),
    COMMENTAIRE         CLOB,
    constraint PK_EMBARQUEMENT
        primary key (ID_SATELLITE, REF_INSTRUMENT)
)
/

create index IDX_EMB_REF_INSTRUMENT
    on EMBARQUEMENT (REF_INSTRUMENT)
/

create table AFFECTATION_STATION
(
    ID_CENTRE        VARCHAR2(20) not null
        constraint FK_AFF_CENTRE
            references CENTRE_CONTROLE,
    CODE_STATION     VARCHAR2(20) not null
        constraint FK_AFF_STATION
            references STATION_SOL,
    DATE_AFFECTATION DATE         not null,
    COMMENTAIRE      CLOB,
    constraint PK_AFFECTATION_STATION
        primary key (ID_CENTRE, CODE_STATION)
)
/

create index IDX_AFF_CODE_STATION
    on AFFECTATION_STATION (CODE_STATION)
/

create table PARTICIPATION
(
    ID_SATELLITE   VARCHAR2(20)  not null
        constraint FK_PART_SATELLITE
            references SATELLITE,
    ID_MISSION     VARCHAR2(20)  not null
        constraint FK_PART_MISSION
            references MISSION,
    ROLE_SATELLITE VARCHAR2(100) not null,
    COMMENTAIRE    CLOB,
    constraint PK_PARTICIPATION
        primary key (ID_SATELLITE, ID_MISSION)
)
/

create index IDX_PART_ID_MISSION
    on PARTICIPATION (ID_MISSION)
/

create materialized view MV_VOLUMES_MENSUELS
    refresh force on demand
as
SELECT
    TRUNC(f.datetime_debut, 'MM') AS mois,
    c.nom_centre,
    s.format_cubesat,
    SUM(NVL(f.volume_donnees, 0)) AS volume_total
FROM FENETRE_COM f
         JOIN SATELLITE s
              ON f.id_satellite = s.id_satellite
         JOIN STATION_SOL st
              ON f.code_station = st.code_station
         JOIN AFFECTATION_STATION af
              ON st.code_station = af.code_station
         JOIN CENTRE_CONTROLE c
              ON af.id_centre = c.id_centre
WHERE f.statut = 'Réalisée'
GROUP BY
    TRUNC(f.datetime_debut, 'MM'),
    c.nom_centre,
    s.format_cubesat
/

create view V_SATELLITES_OPERATIONNELS as
SELECT
    s.id_satellite,
    s.nom_satellite,
    s.format_cubesat,
    s.capacite_batterie,
    s.statut,
    o.type_orbite,
    o.altitude,
    COUNT(e.ref_instrument) AS nb_instruments
FROM SATELLITE s
         JOIN ORBITE o
              ON s.id_orbite = o.id_orbite
         LEFT JOIN EMBARQUEMENT e
                   ON s.id_satellite = e.id_satellite
WHERE s.statut = 'Opérationnel'
GROUP BY
    s.id_satellite,
    s.nom_satellite,
    s.format_cubesat,
    s.capacite_batterie,
    s.statut,
    o.type_orbite,
    o.altitude
/

create view V_FENETRES_DETAIL as
SELECT
    f.id_fenetre,
    f.datetime_debut,
    f.duree,
    TO_CHAR(NUMTODSINTERVAL(f.duree, 'SECOND'), 'HH24:MI:SS') AS duree_formatee,
    f.volume_donnees,
    f.statut,
    s.nom_satellite,
    st.nom_station,
    c.nom_centre
FROM FENETRE_COM f
         JOIN SATELLITE s
              ON f.id_satellite = s.id_satellite
         JOIN STATION_SOL st
              ON f.code_station = st.code_station
         LEFT JOIN AFFECTATION_STATION af
                   ON st.code_station = af.code_station
         LEFT JOIN CENTRE_CONTROLE c
                   ON af.id_centre = c.id_centre
/

create view V_STATS_MISSIONS as
SELECT
    m.id_mission,
    m.nom_mission,
    COUNT(DISTINCT p.id_satellite) AS nb_satellites,
    COUNT(DISTINCT o.type_orbite) AS nb_types_orbite,
    NVL(SUM(f.volume_donnees), 0) AS volume_total
FROM MISSION m
         LEFT JOIN PARTICIPATION p
                   ON m.id_mission = p.id_mission
         LEFT JOIN SATELLITE s
                   ON p.id_satellite = s.id_satellite
         LEFT JOIN ORBITE o
                   ON s.id_orbite = o.id_orbite
         LEFT JOIN FENETRE_COM f
                   ON s.id_satellite = f.id_satellite
GROUP BY
    m.id_mission,
    m.nom_mission
/

create PACKAGE pkg_nanoOrbit IS

    ----------------------------------------------------------------
    -- Type public : statistiques synthétiques d'un satellite
    ----------------------------------------------------------------
    TYPE t_stats_satellite IS RECORD (
        nb_fenetres         NUMBER,
        volume_total        NUMBER,
        duree_moy_secondes  NUMBER
    );

    ----------------------------------------------------------------
    -- Constantes métier publiques
    ----------------------------------------------------------------
    c_statut_min_fenetre CONSTANT SATELLITE.statut%TYPE := 'Opérationnel';
    c_duree_max_fenetre  CONSTANT NUMBER := 900;
    c_seuil_revision     CONSTANT NUMBER := 50;

    ----------------------------------------------------------------
    -- Procédures publiques
    ----------------------------------------------------------------
    PROCEDURE planifier_fenetre(
        p_id_satellite    IN  SATELLITE.id_satellite%TYPE,
        p_code_station    IN  STATION_SOL.code_station%TYPE,
        p_datetime_debut  IN  FENETRE_COM.datetime_debut%TYPE,
        p_duree           IN  FENETRE_COM.duree%TYPE,
        p_id_fenetre      OUT FENETRE_COM.id_fenetre%TYPE
    );

    PROCEDURE cloturer_fenetre(
        p_id_fenetre       IN FENETRE_COM.id_fenetre%TYPE,
        p_volume_donnees   IN FENETRE_COM.volume_donnees%TYPE
    );

    PROCEDURE affecter_satellite_mission(
        p_id_satellite IN PARTICIPATION.id_satellite%TYPE,
        p_id_mission   IN PARTICIPATION.id_mission%TYPE,
        p_role         IN PARTICIPATION.role_satellite%TYPE
    );

    PROCEDURE mettre_en_revision(
        p_id_satellite IN SATELLITE.id_satellite%TYPE
    );

    ----------------------------------------------------------------
    -- Fonctions publiques
    ----------------------------------------------------------------
    FUNCTION calculer_volume_theorique(
        p_id_fenetre IN FENETRE_COM.id_fenetre%TYPE
    ) RETURN NUMBER;

    FUNCTION statut_constellation
    RETURN VARCHAR2;

    FUNCTION stats_satellite(
        p_id_satellite IN SATELLITE.id_satellite%TYPE
    ) RETURN t_stats_satellite;

END pkg_nanoOrbit;
/

create PROCEDURE afficher_statut_satellite (
    p_id IN SATELLITE.id_satellite%TYPE
)
IS
    v_nom      SATELLITE.nom_satellite%TYPE;
    v_statut   SATELLITE.statut%TYPE;
    v_orbite   SATELLITE.id_orbite%TYPE;
BEGIN
    SELECT nom_satellite, statut, id_orbite
    INTO v_nom, v_statut, v_orbite
    FROM SATELLITE
    WHERE id_satellite = p_id;

    DBMS_OUTPUT.PUT_LINE('Satellite : ' || v_nom);
    DBMS_OUTPUT.PUT_LINE('Statut    : ' || v_statut);
    DBMS_OUTPUT.PUT_LINE('Orbite    : ' || v_orbite);
    DBMS_OUTPUT.PUT_LINE('Instruments :');

    FOR rec IN (
        SELECT e.ref_instrument, i.type_instrument, e.etat_fonctionnement
        FROM EMBARQUEMENT e
        JOIN INSTRUMENT i
          ON i.ref_instrument = e.ref_instrument
        WHERE e.id_satellite = p_id
    )
    LOOP
        DBMS_OUTPUT.PUT_LINE(
            '- ' || rec.ref_instrument || ' / ' ||
            rec.type_instrument || ' / ' ||
            rec.etat_fonctionnement
        );
    END LOOP;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Satellite introuvable : ' || p_id);
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Erreur : ' || SQLERRM);
END;
/

create PROCEDURE mettre_a_jour_statut (
    p_id              IN  SATELLITE.id_satellite%TYPE,
    p_statut          IN  SATELLITE.statut%TYPE,
    p_ancien_statut   OUT SATELLITE.statut%TYPE
)
IS
BEGIN
    SELECT statut
    INTO p_ancien_statut
    FROM SATELLITE
    WHERE id_satellite = p_id;

    UPDATE SATELLITE
    SET statut = p_statut
    WHERE id_satellite = p_id;

    IF SQL%ROWCOUNT = 0 THEN
        RAISE_APPLICATION_ERROR(-20020, 'Aucune mise à jour effectuée.');
    END IF;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20021, 'Satellite introuvable.');
    WHEN OTHERS THEN
        RAISE;
END;
/

create FUNCTION calculer_volume_session (
    p_id_fenetre IN FENETRE_COM.id_fenetre%TYPE
)
RETURN NUMBER
IS
    v_duree      FENETRE_COM.duree%TYPE;
    v_debit      STATION_SOL.debit_max%TYPE;
    v_volume     NUMBER;
BEGIN
    SELECT f.duree, s.debit_max
    INTO v_duree, v_debit
    FROM FENETRE_COM f
    JOIN STATION_SOL s
      ON s.code_station = f.code_station
    WHERE f.id_fenetre = p_id_fenetre;

    v_volume := v_duree * v_debit;

    RETURN v_volume;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20030, 'Fenêtre introuvable.');
END;
/


