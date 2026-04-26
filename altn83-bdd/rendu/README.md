# NanoOrbit — CubeSat Earth Observation System

**Projet base de données réparties · Module ALTN83 · EFREI Paris Panthéon-Assas**  
**Auteurs** : Valentin LE GALL & Elia LEPREVOST  
**SGBD** : Oracle 23ai — schéma `NANOORBIT_ADMIN` sur `FREEPDB1`  
**Encadrant** : Christophe CROISANT

---

## Table des matières

1. [Contexte et présentation du projet](#1-contexte-et-présentation-du-projet)
2. [Architecture distribuée multi-sites](#2-architecture-distribuée-multi-sites)
3. [Phase 1 — Conception & Modélisation MERISE](#3-phase-1--conception--modélisation-merise)
   - [Dictionnaire des données](#31-dictionnaire-des-données)
   - [Modèle Conceptuel de Données (MCD)](#32-modèle-conceptuel-de-données-mcd)
   - [Modèle Logique de Données (MLD)](#33-modèle-logique-de-données-mld)
   - [Choix de modélisation délicats](#34-choix-de-modélisation-délicats)
   - [Réflexion sur la répartition](#35-réflexion-sur-la-répartition)
4. [Phase 2 — Schéma Oracle & Triggers](#4-phase-2--schéma-oracle--triggers)
   - [DDL — Création des tables](#41-ddl--création-des-tables)
   - [DML — Jeu de données de référence](#42-dml--jeu-de-données-de-référence)
   - [Triggers métier](#43-triggers-métier)
5. [Phase 3 — PL/SQL & Package pkg_nanoOrbit](#5-phase-3--plsql--package-pkg_nanoorbit)
   - [Paliers 1 à 5 — Exercices progressifs](#51-paliers-1-à-5--exercices-progressifs)
   - [Procédures et fonctions standalone](#52-procédures-et-fonctions-standalone)
   - [Package pkg_nanoOrbit](#53-package-pkg_nanoorbit)
6. [Phase 4 — Exploitation avancée & Optimisation](#6-phase-4--exploitation-avancée--optimisation)
   - [Vues (simples et matérialisée)](#61-vues-simples-et-matérialisée)
   - [CTE et sous-requêtes avancées](#62-cte-et-sous-requêtes-avancées)
   - [Fonctions analytiques OVER](#63-fonctions-analytiques-over)
   - [MERGE INTO — Synchronisation](#64-merge-into--synchronisation)
   - [Index & EXPLAIN PLAN](#65-index--explain-plan)
7. [Règles métier complètes](#7-règles-métier-complètes)
8. [Correspondance avec les TPs](#8-correspondance-avec-les-tps)

---

## 1. Contexte et présentation du projet

La startup **NanoOrbit** exploite une constellation de **CubeSats** pour surveiller des zones climatiques sensibles : déforestation, fonte des glaces, qualité de l'air, évolution du trait de côte. Pour piloter cette constellation, elle a besoin d'un système d'information capable de :

- Gérer une flotte de satellites miniaturisés (formats 1U à 12U)
- Planifier et tracer les fenêtres de communication avec les stations au sol
- Administrer les missions scientifiques et les instruments embarqués
- Garantir la cohérence des données par des mécanismes actifs (triggers, procédures)
- Préparer une architecture de données distribuée par zone géographique

Le projet couvre **six domaines fonctionnels** :

| Domaine | Description |
|---|---|
| Flotte de satellites | Caractéristiques techniques, statut opérationnel |
| Orbites | Paramètres orbitaux, type d'orbite, couverture géographique |
| Instruments embarqués | Capteurs, caméras, équipements scientifiques |
| Stations au sol | Localisation, capacités de communication |
| Fenêtres de communication | Créneaux de passage, volumes échangés |
| Missions scientifiques | Objectifs, zones cibles, participation des satellites |

Ce projet constitue le projet de validation du module ALTN83. Il est organisé en **quatre phases progressives** : modélisation MERISE, implémentation Oracle, programmation PL/SQL et exploitation avancée.

> **TP associé** : `TP1 — Modélisation & VLS` introduit la méthode MERISE et la construction d'un MCD à partir d'un cahier des charges. Le projet NanoOrbit applique directement ces compétences à un cas réel multi-entités.

---

## 2. Architecture distribuée multi-sites

NanoOrbit opère depuis **trois centres de contrôle** géographiquement distincts :

| Code | Nom | Ville | Région | Fuseau | Responsabilité |
|---|---|---|---|---|---|
| CTR-001 | NanoOrbit Paris HQ | Paris | Europe | Europe/Paris (UTC+1) | Supervision GS-TLS-01 (Toulouse) et GS-KIR-01 (Kiruna) — missions polaires SSO |
| CTR-002 | NanoOrbit Houston | Houston | Amériques | America/Chicago (UTC−5) | Centre de backup et coordination des missions équatoriales (orbites LEO) |
| CTR-003 | NanoOrbit Singapore | Singapour | Asie-Pacifique | Asia/Singapore (UTC+8) | Supervision GS-SGP-01 (Singapour) — couverture zone indo-pacifique |

Chaque centre gère ses propres stations au sol et fenêtres de communication, mais **partage un référentiel global** de satellites, d'orbites et de missions.

### Fragmentation des données

| Table | Nature | Justification |
|---|---|---|
| `SATELLITE`, `ORBITE`, `MISSION`, `INSTRUMENT`, `EMBARQUEMENT`, `PARTICIPATION`, `HISTORIQUE_STATUT` | **Globale** — répliquée sur tous les sites | Référentiel commun de la constellation |
| `FENETRE_COM` | **Locale** — fragmentée horizontalement par station | Planification quotidienne propre à chaque centre |
| `STATION_SOL`, `AFFECTATION_STATION` | **Hybride** — globale logiquement, fragmentée physiquement par centre | Chaque centre travaille sur ses propres stations |

### Risques de cohérence identifiés

- **Scénario 1 — Mise à jour simultanée du statut** : Paris et Houston modifient en même temps le statut de SAT-003 (ex. Opérationnel ↔ En veille). Sans verrouillage distribué, cela produit un conflit d'écriture multi-sites.
- **Scénario 2 — Double planification de fenêtre** : deux centres créent simultanément une fenêtre pour le même satellite sur des horaires incompatibles. Sans synchronisation transactionnelle, cela crée un chevauchement métier incohérent.

> **TP associé** : `TP1 — Étape 4` traite exactement ces questions (Q1 à Q4 sur la répartition) : tables locales vs globales, mécanismes de synchronisation et continuité de service en cas de panne du serveur central.

---

## 3. Phase 1 — Conception & Modélisation MERISE

### 3.1 Dictionnaire des données

Le dictionnaire recense l'ensemble des attributs du système avec leurs types Oracle, contraintes et remarques.

#### Table SATELLITE

| Attribut | Type Oracle | Obligatoire | Unique | Contrainte / Remarque |
|---|---|---|---|---|
| `id_satellite` | `VARCHAR2(20)` | OUI | OUI | PK. Code alphanumérique immuable (format SAT-NNN) |
| `nom_satellite` | `VARCHAR2(100)` | OUI | OUI | Nom commercial ou opérationnel |
| `date_lancement` | `DATE` | OUI | NON | Date effective de mise en orbite |
| `masse` | `NUMBER(5,2)` | OUI | NON | Masse au lancement en kg — CHECK > 0 |
| `format_cubesat` | `VARCHAR2(5)` | OUI | NON | 1U / 3U / 6U / 12U — 1U = 10×10×10 cm |
| `statut` | `VARCHAR2(30)` | OUI | NON | Opérationnel / En veille / Défaillant / Désorbité |
| `duree_vie_prevue` | `NUMBER(5)` | OUI | NON | Durée nominale de la mission en mois |
| `capacite_batterie` | `NUMBER(6,1)` | OUI | NON | Énergie stockable en Wh — CHECK > 0 |
| `id_orbite` | `VARCHAR2(20)` | OUI | NON | FK → ORBITE |

#### Table ORBITE

| Attribut | Type Oracle | Contrainte / Remarque |
|---|---|---|
| `id_orbite` | `VARCHAR2(20)` | PK |
| `type_orbite` | `VARCHAR2(10)` | CHECK IN ('LEO','MEO','SSO','GEO') |
| `altitude` | `NUMBER(5)` | CHECK > 0 — altitude nominale en km |
| `inclinaison` | `NUMBER(5,2)` | CHECK BETWEEN 0 AND 180 — angle en degrés |
| `periode_orbitale` | `NUMBER(6,2)` | CHECK > 0 — durée d'une révolution en minutes |
| `excentricite` | `NUMBER(6,4)` | CHECK >= 0 AND < 1 — 0 = circulaire |
| `zone_couverture` | `VARCHAR2(200)` | Nullable — description géographique |

Contrainte UNIQUE sur le couple `(altitude, inclinaison)` — règle RG-O02.

#### Table INSTRUMENT

| Attribut | Type Oracle | Contrainte / Remarque |
|---|---|---|
| `ref_instrument` | `VARCHAR2(20)` | PK — référence catalogue constructeur |
| `type_instrument` | `VARCHAR2(50)` | Caméra optique / Infrarouge / Récepteur AIS / Spectromètre |
| `modele` | `VARCHAR2(100)` | Désignation commerciale |
| `resolution` | `NUMBER(6,1)` | Nullable — NULL si non applicable (ex. AIS) |
| `consommation` | `NUMBER(5,2)` | CHECK > 0 — puissance consommée en W |
| `masse` | `NUMBER(5,3)` | CHECK > 0 — masse en kg |

#### Table FENETRE_COM

| Attribut | Type Oracle | Contrainte / Remarque |
|---|---|---|
| `id_fenetre` | `NUMBER(10)` | PK auto-incrémentée via séquence `SEQ_FENETRE` |
| `datetime_debut` | `TIMESTAMP(6)` | Début du passage du satellite au-dessus de la station |
| `duree` | `NUMBER(4)` | CHECK BETWEEN 1 AND 900 — durée en secondes (règle RG-F04) |
| `elevation_max` | `NUMBER(5,2)` | CHECK BETWEEN 0 AND 90 — angle d'élévation maximal |
| `volume_donnees` | `NUMBER(8,1)` | Nullable — NULL si statut ≠ 'Réalisée' (règle RG-F05) |
| `statut` | `VARCHAR2(20)` | CHECK IN ('Planifiée','Réalisée') |
| `code_station` | `VARCHAR2(20)` | FK → STATION_SOL |
| `id_satellite` | `VARCHAR2(20)` | FK → SATELLITE |

#### Table HISTORIQUE_STATUT

Table technique créée pour la traçabilité — alimentée automatiquement par le trigger T5.

| Attribut | Type Oracle | Contrainte / Remarque |
|---|---|---|
| `id_historique` | `NUMBER(10)` | PK auto-incrémentée via séquence `SEQ_HISTORIQUE_STATUT` |
| `statut` | `VARCHAR2(30)` | CHECK IN ('Opérationnel','En veille','Défaillant','Désorbité') |
| `timestamp_` | `TIMESTAMP(6)` | Horodatage du changement |
| `id_satellite` | `VARCHAR2(20)` | FK → SATELLITE |

#### Tables de liaison

| Table | Clé primaire composite | Attributs propres |
|---|---|---|
| `EMBARQUEMENT` | (`id_satellite`, `ref_instrument`) | `date_integration DATE`, `etat_fonctionnement VARCHAR2(20)`, `commentaire CLOB` |
| `AFFECTATION_STATION` | (`id_centre`, `code_station`) | `date_affectation DATE`, `commentaire CLOB` |
| `PARTICIPATION` | (`id_satellite`, `id_mission`) | `role_satellite VARCHAR2(100)`, `commentaire CLOB` |

### 3.2 Modèle Conceptuel de Données (MCD)

Le MCD suit la méthode MERISE. Les entités principales sont :

```
SATELLITE ──────(EMBARQUEMENT)────── INSTRUMENT
    │                                      
    │──────(PARTICIPATION)────── MISSION   
    │                                      
    ├──── ORBITE                           
    │                                      
    └──────(FENETRE_COM)────── STATION_SOL ──(AFFECTATION_STATION)── CENTRE_CONTROLE
    │
    └──── HISTORIQUE_STATUT
```

**Associations porteuses d'attributs** (points délicats MERISE) :
- `EMBARQUEMENT` entre SATELLITE et INSTRUMENT porte `date_integration` et `etat_fonctionnement`
- `PARTICIPATION` entre SATELLITE et MISSION porte `role_satellite`
- `AFFECTATION_STATION` entre CENTRE_CONTROLE et STATION_SOL porte `date_affectation`

**Cardinalités** :
- SATELLITE — ORBITE : N,1 (un satellite sur une seule orbite, une orbite pour plusieurs satellites)
- SATELLITE — INSTRUMENT via EMBARQUEMENT : N,N
- SATELLITE — MISSION via PARTICIPATION : N,N
- SATELLITE — FENETRE_COM : 1,N
- STATION_SOL — FENETRE_COM : 1,N
- CENTRE_CONTROLE — STATION_SOL via AFFECTATION_STATION : N,N

> **TP associé** : `TP1 — Étape 2` détaille la construction du MCD MERISE avec la gestion des associations porteuses d'attributs. La relation FENETRE_COM (ternaire ou binaire ?) y est explicitement discutée.

### 3.3 Modèle Logique de Données (MLD)

Le MLD en notation textuelle (PK souligné, FK précédé de #) :

```
ORBITE          = (id_orbite, type_orbite, altitude, inclinaison, periode_orbitale, excentricite, zone_couverture)
SATELLITE       = (id_satellite, nom_satellite, date_lancement, masse, format_cubesat, statut, duree_vie_prevue, capacite_batterie, #id_orbite)
INSTRUMENT      = (ref_instrument, type_instrument, modele, resolution, consommation, masse)
CENTRE_CONTROLE = (id_centre, nom_centre, ville, region_geo, fuseau_horaire, statut)
STATION_SOL     = (code_station, nom_station, latitude, longitude, diametre_antenne, bande_frequence, debit_max, statut)
MISSION         = (id_mission, nom_mission, objectif, zone_geo_cible, date_debut, date_fin, statut_mission)
FENETRE_COM     = (id_fenetre, datetime_debut, duree, elevation_max, volume_donnees, statut, #code_station, #id_satellite)
HISTORIQUE_STATUT = (id_historique, statut, timestamp_, #id_satellite)
EMBARQUEMENT    = (#id_satellite, #ref_instrument, date_integration, etat_fonctionnement, commentaire)
AFFECTATION_STATION = (#id_centre, #code_station, date_affectation, commentaire)
PARTICIPATION   = (#id_satellite, #id_mission, role_satellite, commentaire)
```

La **3NF est respectée** sur toutes les tables : aucune dépendance transitive n'existe entre attributs non-clé.

> **TP associé** : `TP1 — Étape 3` guide la dérivation MCD → MLD en appliquant les règles de passage MERISE (entités → tables, associations porteuses → tables de liaison avec PK composite, cardinalités → FK).

### 3.4 Choix de modélisation délicats

#### Choix 1 — Association EMBARQUEMENT

Le premier choix délicat concerne la relation entre SATELLITE et INSTRUMENT. Les attributs `date_integration`, `etat_fonctionnement` et `commentaire` dépendent du **couple** (satellite, instrument) et non de l'un ou l'autre seul. EMBARQUEMENT est donc modélisée comme une **association porteuse d'attributs**, transformée en table avec clé primaire composite. Ce choix garantit la 3NF et permet de tracer l'état spécifique d'un instrument sur chaque satellite.

#### Choix 2 — Modélisation de FENETRE_COM

La relation entre SATELLITE et STATION_SOL est modélisée comme une **entité autonome** `FENETRE_COM` (et non une simple association binaire). Ce choix permet d'historiser les passages, de stocker les attributs métier (`datetime_debut`, `duree`, `elevation_max`, `volume_donnees`, `statut`) et d'implémenter les contraintes procédurales de non-chevauchement (T2). Cette approche est cohérente avec les règles RG-F01 à RG-F05.

#### Choix 3 — Historisation du statut satellite

La table `HISTORIQUE_STATUT` a été simplifiée à 3 attributs : `(id_historique, statut, timestamp_, #id_satellite)`. La première version incluait `previous_statut` et `current_statut`, ce qui introduisait une redondance potentielle contraire à la 3NF. Dans la version retenue, chaque ligne représente un état à un instant donné, et le statut précédent se déduit par tri chronologique.

### 3.5 Réflexion sur la répartition

#### Q1 — Tables strictement locales

`FENETRE_COM` est strictement locale : elle correspond à la planification opérationnelle quotidienne des communications entre un satellite et les stations supervisées par le centre concerné. `STATION_SOL` et `AFFECTATION_STATION` peuvent aussi être fragmentées horizontalement par zone géographique ou par centre.

#### Q2 — Tables globales

`SATELLITE`, `ORBITE`, `MISSION`, `INSTRUMENT`, `EMBARQUEMENT`, `PARTICIPATION`, `CENTRE_CONTROLE` et `HISTORIQUE_STATUT` constituent le référentiel commun. Mécanismes proposés :
- **Réplication multi-maître** pour SATELLITE, MISSION, HISTORIQUE_STATUT (modifiables depuis plusieurs sites)
- **Réplication lecture seule** pour ORBITE, INSTRUMENT, CENTRE_CONTROLE (mises à jour rares)
- **Fragmentation + synchronisation asynchrone** pour STATION_SOL, AFFECTATION_STATION, FENETRE_COM

#### Q3 — Continuité si serveur central indisponible

Proposition de **fragmentation horizontale de FENETRE_COM** par station / zone géographique :
- Fragment Europe → stations GS-TLS-01 (Toulouse) et GS-KIR-01 (Kiruna)
- Fragment Asie → station GS-SGP-01 (Singapour)
- Fragment Amériques → stations supervisées par Houston

Chaque site conserve localement ses fenêtres et synchronise de façon différée vers le référentiel central dès le rétablissement du lien.

#### Q4 — Risques de cohérence

Voir section [Architecture distribuée](#2-architecture-distribuée-multi-sites).

---

## 4. Phase 2 — Schéma Oracle & Triggers

> **TP associé** : `TP2 — Schéma VLS & Triggers` enseigne la syntaxe DDL Oracle (CREATE TABLE, contraintes, index), le DML (INSERT dans le bon ordre référentiel) et les triggers PL/SQL (BEFORE/AFTER, :NEW/:OLD, RAISE_APPLICATION_ERROR). La Phase 2 du projet applique directement ces compétences.

### 4.1 DDL — Création des tables

Les tables doivent être créées dans un **ordre strict imposé par les dépendances de clés étrangères** :

| # | Table | Dépend de | Contenu |
|---|---|---|---|
| 1 | `ORBITE` | — | Référentiel des plans orbitaux |
| 2 | `SATELLITE` | ORBITE | Parc de CubeSats |
| 3 | `INSTRUMENT` | — | Catalogue des instruments |
| 4 | `EMBARQUEMENT` | SATELLITE, INSTRUMENT | Instruments montés sur satellites |
| 5 | `CENTRE_CONTROLE` | — | Centres d'opération NanoOrbit |
| 6 | `STATION_SOL` | — | Stations d'antenne mondiales |
| 7 | `AFFECTATION_STATION` | CENTRE_CONTROLE, STATION_SOL | Rattachement station ↔ centre |
| 8 | `MISSION` | — | Missions scientifiques |
| 9 | `FENETRE_COM` | SATELLITE, STATION_SOL | Créneaux de communication |
| 10 | `PARTICIPATION` | SATELLITE, MISSION | Rôles des satellites dans les missions |
| 11 | `HISTORIQUE_STATUT` | SATELLITE | Traçabilité des changements de statut |

**Pourquoi SATELLITE ne peut pas être créé avant ORBITE ?** La table SATELLITE contient une FK `id_orbite` qui référence la PK de ORBITE. En Oracle, la table référencée doit exister avant la déclaration de la contrainte de FK. Cela traduit la règle RG-S02 : chaque satellite est associé à une et une seule orbite.

#### Contraintes et index par table

**ORBITE** — contraintes notables :
- `UQ_ORBITE_ALT_INC` : `UNIQUE(altitude, inclinaison)` — le couple est unique (RG-O02)
- CHECK sur `type_orbite IN ('LEO','MEO','SSO','GEO')`, `altitude > 0`, `inclinaison BETWEEN 0 AND 180`, `excentricite >= 0 AND < 1`

**SATELLITE** — contraintes et index :
- FK `fk_satellite_orbite` → ORBITE
- `UQ_SATELLITE_NOM` : unicité du nom
- CHECK sur `format_cubesat IN ('1U','3U','6U','12U')`, `statut IN ('Opérationnel','En veille','Défaillant','Désorbité')`
- `IDX_SATELLITE_ID_ORBITE` : accélère les jointures avec ORBITE
- `IDX_SATELLITE_STATUT` : filtre rapide par statut opérationnel
- `IDX_SATELLITE_STATUT_ORBITE` : index composite pour les requêtes filtrant sur les deux colonnes simultanément

**Pourquoi `format_cubesat` est VARCHAR2(5) et pas NUMBER ?** Les valeurs 1U, 3U, 6U, 12U sont des codes alphanumériques (chiffre + lettre U), pas des nombres purs. Un type numérique ne conviendrait pas.

**FENETRE_COM** — contraintes et index :
- FK vers SATELLITE et STATION_SOL
- CHECK `duree BETWEEN 1 AND 900` — un satellite reste au-delà de 900s hors du cône de visibilité de l'antenne (RG-F04)
- CHECK `volume_donnees IS NULL OR volume_donnees >= 0`
- `IDX_FENETRE_CODE_STATION`, `IDX_FENETRE_ID_SATELLITE`, `IDX_FENETRE_DATETIME`
- `IDX_FENETRE_MOIS` : index fonctionnel sur `TRUNC(datetime_debut, 'fmmm')` — optimise le regroupement mensuel utilisé dans la vue matérialisée

**HISTORIQUE_STATUT** — index :
- `IDX_HISTORIQUE_ID_SATELLITE` : recherche de tout l'historique d'un satellite
- `IDX_HISTORIQUE_SAT_TS` : index composite `(id_satellite, timestamp_)` — optimise les requêtes chronologiques du type "dernier statut de SAT-003"

**Pourquoi la règle RG-S06 (satellite désorbité : plus de fenêtre ni de mission) ne peut pas être vérifiée en DDL seul ?** Une contrainte DDL classique (CHECK, NOT NULL, UNIQUE, FK) ne peut pas contrôler une condition dépendant d'une autre table. La solution est un **trigger BEFORE INSERT OR UPDATE** sur FENETRE_COM et PARTICIPATION qui interroge la table SATELLITE.

**Pourquoi la contrainte RG-F02 (non-chevauchement) ne peut pas être exprimée en CHECK ?** Un CHECK ne compare que les valeurs de la ligne courante — il ne peut pas vérifier l'existence d'un recouvrement avec d'autres lignes de la même table. Cette contrainte nécessite un trigger.

#### Séquences Oracle

```sql
SEQ_HISTORIQUE_STATUT   -- alimente id_historique
SEQ_SATELLITE           -- disponible pour générer des ids satellites
SEQ_ORBITE              -- disponible pour générer des ids orbites
SEQ_CENTRE              -- disponible pour générer des ids centres
SEQ_FENETRE             -- alimente id_fenetre
```

Toutes créées avec `NOCACHE` pour garantir l'absence de trous dans un contexte distribué.

### 4.2 DML — Jeu de données de référence

Le jeu de données initial doit être inséré **dans l'ordre strict des dépendances** (même ordre que le DDL) avec un `COMMIT` final.

#### Orbites (3 lignes)

| id_orbite | type | altitude | inclinaison | periode | excentricite | zone_couverture |
|---|---|---|---|---|---|---|
| ORB-001 | SSO | 550 km | 97,6° | 95,5 min | 0,0010 | Polaire globale — Europe / Arctique |
| ORB-002 | SSO | 700 km | 98,2° | 98,8 min | 0,0008 | Polaire globale — haute latitude |
| ORB-003 | LEO | 400 km | 51,6° | 92,6 min | 0,0020 | Équatoriale — zone tropicale |

#### Satellites (5 lignes — dont 1 Désorbité)

| id_satellite | nom | masse | format | statut | duree_vie | batterie | orbite |
|---|---|---|---|---|---|---|---|
| SAT-001 | NanoOrbit-Alpha | 1,30 kg | 3U | Opérationnel | 60 mois | 20 Wh | ORB-001 |
| SAT-002 | NanoOrbit-Beta | 1,30 kg | 3U | Opérationnel | 60 mois | 20 Wh | ORB-001 |
| SAT-003 | NanoOrbit-Gamma | 2,00 kg | 6U | Opérationnel | 84 mois | 40 Wh | ORB-002 |
| SAT-004 | NanoOrbit-Delta | 2,00 kg | 6U | En veille | 84 mois | 40 Wh | ORB-002 |
| SAT-005 | NanoOrbit-Epsilon | 4,50 kg | 12U | **Désorbité** | 36 mois | 80 Wh | ORB-003 |

> SAT-005 est volontairement inclus en statut Désorbité pour tester le trigger T1 (trg_valider_fenetre) et la règle RG-S06. Il participe à la mission terminée MSN-DEF-2022 (ce qui est valide car la mission était active à l'époque).

#### Instruments (4 lignes)

| ref_instrument | type | modele | resolution | consommation | masse |
|---|---|---|---|---|---|
| INS-CAM-01 | Caméra optique | PlanetScope-Mini | 3 m | 2,5 W | 0,40 kg |
| INS-IR-01 | Infrarouge | FLIR-Lepton-3 | 160 m | 1,2 W | 0,15 kg |
| INS-AIS-01 | Récepteur AIS | ShipTrack-V2 | **NULL** | 0,8 W | 0,12 kg |
| INS-SPEC-01 | Spectromètre | HyperSpec-Nano | 30 m | 3,1 W | 0,60 kg |

> INS-AIS-01 a une résolution NULL car un récepteur AIS (Automatic Identification System) ne produit pas d'image. Ce cas illustre la gestion du NULL avec `NVL` (Palier 2, Exercice 4 de la Phase 3).

#### Embarquements (7 lignes)

| satellite | instrument | date_integration | etat_fonctionnement |
|---|---|---|---|
| SAT-001 | INS-CAM-01 | 2022-03-15 | Nominal |
| SAT-001 | INS-IR-01 | 2022-03-15 | Nominal |
| SAT-002 | INS-CAM-01 | 2022-03-15 | Nominal |
| SAT-003 | INS-CAM-01 | 2023-06-10 | Nominal |
| SAT-003 | INS-SPEC-01 | 2023-06-10 | Nominal |
| SAT-004 | INS-IR-01 | 2023-06-10 | **Dégradé** |
| SAT-005 | INS-AIS-01 | 2021-11-20 | **Hors service** |

#### Centres de contrôle (2 lignes dans le jeu initial, CTR-003 ajouté en Phase 4 via MERGE INTO)

| id_centre | nom | ville | region | fuseau | statut |
|---|---|---|---|---|---|
| CTR-001 | NanoOrbit Paris HQ | Paris | Europe | Europe/Paris | Actif |
| CTR-002 | NanoOrbit Houston | Houston | Amériques | America/Chicago | Actif |

#### Stations au sol (3 lignes)

| code_station | nom | latitude | longitude | antenne | bande | debit_max | statut |
|---|---|---|---|---|---|---|---|
| GS-TLS-01 | Toulouse Ground Station | 43,6047° | 1,4442° | 3,5 m | S | 150 Mbps | Active |
| GS-KIR-01 | Kiruna Arctic Station | 67,8557° | 20,2253° | 5,4 m | X | 400 Mbps | Active |
| GS-SGP-01 | Singapore Station | 1,3521° | 103,8198° | 3,0 m | S | 120 Mbps | **Maintenance** |

> GS-SGP-01 est en statut Maintenance pour tester le trigger T1 : toute tentative de planification d'une fenêtre vers cette station doit être bloquée.

#### Affectations station ↔ centre (3 lignes)

| id_centre | code_station | date_affectation | commentaire |
|---|---|---|---|
| CTR-001 | GS-TLS-01 | 2022-01-10 | Paris HQ supervise Toulouse — proximité géographique |
| CTR-001 | GS-KIR-01 | 2022-01-10 | Paris HQ supervise également Kiruna — missions polaires SSO |
| CTR-002 | GS-SGP-01 | 2023-03-15 | Houston supervise Singapour — couverture zone Asie-Pacifique |

#### Missions (3 lignes)

| id_mission | nom | zone_geo_cible | date_debut | date_fin | statut |
|---|---|---|---|---|---|
| MSN-ARC-2023 | ArcticWatch 2023 | Arctique / Groenland | 2023-01-01 | NULL | **Active** |
| MSN-DEF-2022 | DeforestAlert | Amazonie / Congo | 2022-06-01 | 2023-05-31 | **Terminée** |
| MSN-COAST-2024 | CoastGuard 2024 | Méditerranée / Atlantique | 2024-03-01 | NULL | **Active** |

#### Fenêtres de communication (5 lignes)

| id | datetime_debut | duree | elevation_max | volume_donnees | statut | satellite | station |
|---|---|---|---|---|---|---|---|
| 1 | 2024-01-15 09:14:00 | 420 s | 82,3° | 1 250 Mo | **Réalisée** | SAT-001 | GS-KIR-01 |
| 2 | 2024-01-15 11:52:00 | 310 s | 67,1° | 890 Mo | **Réalisée** | SAT-002 | GS-TLS-01 |
| 3 | 2024-01-16 08:30:00 | 540 s | 88,9° | 1 680 Mo | **Réalisée** | SAT-003 | GS-KIR-01 |
| 4 | 2024-01-20 14:22:00 | 380 s | 71,4° | NULL | **Planifiée** | SAT-001 | GS-TLS-01 |
| 5 | 2024-01-21 07:45:00 | 290 s | 59,8° | NULL | **Planifiée** | SAT-003 | GS-TLS-01 |

> Les fenêtres Planifiées ont `volume_donnees = NULL` (appliqué par le trigger T3). Les plages horaires sont conçues pour ne pas se chevaucher (ni par satellite, ni par station) afin de respecter les règles RG-F02 et RG-F03.

#### Participations (7 lignes)

| satellite | mission | role_satellite |
|---|---|---|
| SAT-001 | MSN-ARC-2023 | Imageur principal |
| SAT-002 | MSN-ARC-2023 | Imageur secondaire |
| SAT-003 | MSN-ARC-2023 | Satellite de relais |
| SAT-001 | MSN-DEF-2022 | Imageur principal |
| SAT-005 | MSN-DEF-2022 | Imageur secondaire |
| SAT-003 | MSN-COAST-2024 | Imageur principal |
| SAT-004 | MSN-COAST-2024 | Satellite de secours |

### 4.3 Triggers métier

Les triggers implémentent les **règles métier qui ne peuvent pas être exprimées par des contraintes statiques** (CHECK, FK, UNIQUE). Cinq triggers sont définis :

#### T1 — trg_valider_fenetre

```
Événement : BEFORE INSERT ON FENETRE_COM
Niveau    : ligne (FOR EACH ROW)
Règles    : RG-S06, RG-G03
```

Bloque la création d'une fenêtre de communication si :
- Le satellite est au statut 'Désorbité' → erreur ORA-20001
- La station est au statut 'Maintenance' → erreur ORA-20002

Ce trigger est déclenché implicitement à chaque appel de la procédure `planifier_fenetre` du package.

#### T2 — trg_no_chevauchement

```
Événement : BEFORE INSERT OR UPDATE ON FENETRE_COM
Niveau    : ligne (FOR EACH ROW)
Règles    : RG-F02, RG-F03
```

Vérifie l'absence de chevauchement temporel pour un même satellite ET pour une même station. Un chevauchement existe si :

```sql
datetime_debut_existante < :NEW.datetime_debut + duree_en_fraction_de_jour
AND datetime_debut_existante + duree_existante > :NEW.datetime_debut
```

Si un conflit est détecté, le trigger lève une erreur avec `RAISE_APPLICATION_ERROR(-20003, ...)`.

#### T3 — trg_volume_realise

```
Événement : BEFORE INSERT OR UPDATE ON FENETRE_COM
Niveau    : ligne (FOR EACH ROW)
Règle     : RG-F05
```

Force `volume_donnees = NULL` si le statut de la fenêtre est différent de 'Réalisée'. Ainsi, les fenêtres 'Planifiées' ne peuvent jamais avoir un volume renseigné.

#### T4 — trg_mission_terminee

```
Événement : BEFORE INSERT ON PARTICIPATION
Niveau    : ligne (FOR EACH ROW)
Règle     : RG-M04
```

Bloque l'ajout d'un satellite à une mission si cette mission est au statut 'Terminée'. Interroge la table MISSION pour récupérer le statut avant d'autoriser l'insertion.

#### T5 — trg_historique_statut

```
Événement : AFTER UPDATE OF statut ON SATELLITE
Niveau    : ligne (FOR EACH ROW)
Règle     : RG-S06 (traçabilité)
```

Trace automatiquement tout changement de statut dans la table `HISTORIQUE_STATUT`. Déclenché par le trigger AFTER, il utilise `:OLD.statut` et `:NEW.statut` pour créer une entrée d'historique horodatée. Déclenché implicitement à chaque appel de `mettre_en_revision` ou `mettre_a_jour_statut`.

---

## 5. Phase 3 — PL/SQL & Package pkg_nanoOrbit

> **TP associé** : `TP3 — PL/SQL` suit exactement les 6 paliers de progression : blocs anonymes, variables & types, structures de contrôle, curseurs, procédures/fonctions, et package. Les exercices 1 à 16 du projet correspondent directement aux exercices du TP.

### 5.1 Paliers 1 à 5 — Exercices progressifs

#### Palier 1 — Bloc anonyme (Ex. 1 et 2)

**Ex. 1** : Bloc anonyme avec `SELECT COUNT(*) INTO` pour compter satellites, stations et missions. Affichage via `DBMS_OUTPUT.PUT_LINE`. Résultat obtenu :
```
*** Bienvenue dans NanoOrbit ***
Nombre de satellites : 5
Nombre de stations   : 3
Nombre de missions   : 3
```

**Ex. 2** : Récupération des caractéristiques de SAT-001 avec `SELECT INTO` et variables typées `%TYPE`. Illustre l'usage de `SATELLITE.nom_satellite%TYPE`.

#### Palier 2 — Variables et types (Ex. 3 et 4)

**Ex. 3** : Lecture d'un enregistrement complet avec `SATELLITE%ROWTYPE`. Permet d'accéder à tous les attributs d'une ligne via une seule variable (`v_sat.nom_satellite`, `v_sat.statut`, etc.).

**Ex. 4** : Gestion des NULL avec `NVL`. Affiche la résolution de INS-AIS-01 en remplaçant NULL par 'N/A'. Illustre la conversion `TO_CHAR(resolution)` puis `NVL(..., 'N/A')`.

#### Palier 3 — Structures de contrôle (Ex. 5, 6, 7)

**Ex. 5** : Structure `IF/ELSIF/ELSE` sur le statut de SAT-005. SAT-005 étant Désorbité, affiche "Satellite hors service définitif."

**Ex. 6** : Instruction `CASE` sur le type d'orbite de ORB-001 (SSO). Affiche "Orbite adaptée à l'observation polaire." Plus lisible qu'une succession de IF lorsque les cas sont fixes.

**Ex. 7** : Boucle `FOR` simulant des durées de passage (5, 10, 15 minutes) et calculant le volume transféré à 2,5 Mo/s :
```
Durée :  5 min ->  Volume :  750 Mo
Durée : 10 min -> Volume : 1500 Mo
Durée : 15 min -> Volume : 2250 Mo
```

#### Palier 4 — Curseurs (Ex. 8, 9, 10, 11)

**Ex. 8** : `SQL%ROWCOUNT` après un UPDATE du statut de SAT-004 — affiche le nombre de lignes réellement modifiées.

**Ex. 9** : **Curseur implicite** (Cursor FOR Loop) — parcourt tous les satellites et affiche id, nom, statut. La boucle `FOR rec IN (SELECT ...)` gère automatiquement OPEN/FETCH/CLOSE. Résultat :
```
SAT-001 - NanoOrbit-Alpha - Opérationnel
SAT-002 - NanoOrbit-Beta  - Opérationnel
SAT-003 - NanoOrbit-Gamma - Opérationnel
SAT-004 - NanoOrbit-Delta - En veille
SAT-005 - NanoOrbit-Epsilon - Désorbité
```

**Ex. 10** : **Curseur explicite** (OPEN/FETCH/EXIT WHEN %NOTFOUND/CLOSE) — même résultat mais contrôle fin sur le parcours.

**Ex. 11** : **Curseur paramétré** `c_fenetres(p_station VARCHAR2)` — liste les fenêtres d'une station donnée avec volume (NVL → 'N/A' pour les Planifiées). Résultat pour GS-TLS-01 :
```
Fenêtre 2 - Volume : 890   - Statut : Réalisée
Fenêtre 4 - Volume : N/A   - Statut : Planifiée
Fenêtre 5 - Volume : N/A   - Statut : Planifiée
```

#### Palier 5 — Procédures et fonctions (Ex. 12, 13, 14, 15, 16)

**Ex. 12** : `NO_DATA_FOUND` et `OTHERS` — SELECT INTO sur SAT-999 (inexistant) → "Aucun satellite ne correspond à cet identifiant."

**Ex. 13** : Validation métier avec `RAISE_APPLICATION_ERROR` avant insertion d'une fenêtre. Vérifie que le satellite est Opérationnel et la station Active. Test sur SAT-005 (Désorbité) → `ORA-20010: Satellite non autorisé pour une nouvelle fenêtre.`

### 5.2 Procédures et fonctions standalone

Ces sous-programmes standalone constituent le **socle minimum du Palier 5** (livrables L3-A) et servent de base au package.

#### `afficher_statut_satellite(p_id IN)`

Affiche le statut, l'orbite et tous les instruments embarqués d'un satellite donné. Utilise un `SELECT INTO` pour les informations principales, puis un curseur `FOR rec IN (SELECT ... FROM EMBARQUEMENT JOIN INSTRUMENT ...)`. Gère `NO_DATA_FOUND` (satellite introuvable) et `OTHERS`.

```sql
-- Appel :
BEGIN afficher_statut_satellite('SAT-001'); END;
-- Résultat :
-- Satellite : NanoOrbit-Alpha
-- Statut    : Opérationnel
-- Orbite    : ORB-001
-- Instruments :
-- - INS-CAM-01 / Caméra optique / Nominal
-- - INS-IR-01 / Infrarouge / Nominal
```

#### `mettre_a_jour_statut(p_id IN, p_statut IN, p_ancien_statut OUT)`

Met à jour le statut d'un satellite et retourne l'ancien statut via un paramètre `OUT`. Vérifie `SQL%ROWCOUNT = 0` pour détecter un satellite introuvable. Le trigger T5 (`trg_historique_statut`) est automatiquement déclenché par le UPDATE, traçant le changement dans `HISTORIQUE_STATUT`.

```sql
-- Appel :
DECLARE v_ancien VARCHAR2(30);
BEGIN mettre_a_jour_statut('SAT-004', 'Opérationnel', v_ancien);
DBMS_OUTPUT.PUT_LINE('Ancien statut : ' || v_ancien); END;
```

#### `calculer_volume_session(p_id_fenetre IN) RETURN NUMBER`

Calcule le volume théorique d'une session en multipliant `duree × debit_max` (débit de la station). Retourne un NUMBER en Mo. Gère `NO_DATA_FOUND` si la fenêtre est introuvable.

```sql
-- Appel :
SELECT calculer_volume_session(1) FROM DUAL;  -- Fenêtre 1 : 420s × 400 Mbps = 168 000 Mo
```

### 5.3 Package pkg_nanoOrbit

Le package encapsule l'ensemble des opérations métier NanoOrbit en un seul objet Oracle compilé.

#### SPEC — Interface publique

```sql
PACKAGE pkg_nanoOrbit IS

    -- Type public : statistiques synthétiques d'un satellite
    TYPE t_stats_satellite IS RECORD (
        nb_fenetres        NUMBER,
        volume_total       NUMBER,
        duree_moy_secondes NUMBER
    );

    -- Constantes métier
    c_statut_min_fenetre CONSTANT SATELLITE.statut%TYPE := 'Opérationnel';
    c_duree_max_fenetre  CONSTANT NUMBER := 900;   -- secondes
    c_seuil_revision     CONSTANT NUMBER := 50;    -- nb fenêtres avant révision

    -- Procédures
    PROCEDURE planifier_fenetre(
        p_id_satellite   IN  SATELLITE.id_satellite%TYPE,
        p_code_station   IN  STATION_SOL.code_station%TYPE,
        p_datetime_debut IN  FENETRE_COM.datetime_debut%TYPE,
        p_duree          IN  FENETRE_COM.duree%TYPE,
        p_id_fenetre     OUT FENETRE_COM.id_fenetre%TYPE
    );

    PROCEDURE cloturer_fenetre(
        p_id_fenetre     IN FENETRE_COM.id_fenetre%TYPE,
        p_volume_donnees IN FENETRE_COM.volume_donnees%TYPE
    );

    PROCEDURE affecter_satellite_mission(
        p_id_satellite IN PARTICIPATION.id_satellite%TYPE,
        p_id_mission   IN PARTICIPATION.id_mission%TYPE,
        p_role         IN PARTICIPATION.role_satellite%TYPE
    );

    PROCEDURE mettre_en_revision(
        p_id_satellite IN SATELLITE.id_satellite%TYPE
    );

    -- Fonctions
    FUNCTION calculer_volume_theorique(
        p_id_fenetre IN FENETRE_COM.id_fenetre%TYPE
    ) RETURN NUMBER;

    FUNCTION statut_constellation RETURN VARCHAR2;

    FUNCTION stats_satellite(
        p_id_satellite IN SATELLITE.id_satellite%TYPE
    ) RETURN t_stats_satellite;

END pkg_nanoOrbit;
```

#### Description des sous-programmes du package

| Sous-programme | Type | Description |
|---|---|---|
| `planifier_fenetre` | Procédure | Insère une fenêtre Planifiée (déclenche T1 + T2 + T3) — retourne l'id généré via OUT |
| `cloturer_fenetre` | Procédure | Passe une fenêtre à 'Réalisée' et enregistre le volume téléchargé |
| `affecter_satellite_mission` | Procédure | Insère une participation (déclenche T4 si mission terminée) |
| `mettre_en_revision` | Procédure | Passe un satellite en 'En veille' (déclenche T5 → HISTORIQUE_STATUT) |
| `calculer_volume_theorique` | Fonction | Retourne `duree × debit_max` de la station pour une fenêtre donnée |
| `statut_constellation` | Fonction | Retourne un résumé textuel ex. "3/5 satellites opérationnels, 3 missions actives" |
| `stats_satellite` | Fonction | Retourne un enregistrement `t_stats_satellite` avec nb fenêtres, volume total, durée moyenne |

#### Scénario de validation du package

```sql
-- 1. Planifier une nouvelle fenêtre pour SAT-001 vers GS-KIR-01
DECLARE v_id FENETRE_COM.id_fenetre%TYPE;
BEGIN
    pkg_nanoOrbit.planifier_fenetre('SAT-001', 'GS-KIR-01',
        TIMESTAMP '2024-02-01 10:00:00', 300, v_id);
    DBMS_OUTPUT.PUT_LINE('Fenêtre créée : ' || v_id);
END;

-- 2. Clôturer avec un volume de données
BEGIN pkg_nanoOrbit.cloturer_fenetre(6, 1500); END;

-- 3. Affecter SAT-004 à la mission MSN-ARC-2023
BEGIN pkg_nanoOrbit.affecter_satellite_mission('SAT-004','MSN-ARC-2023','Satellite de secours'); END;

-- 4. Stats du satellite SAT-001
DECLARE v_stats pkg_nanoOrbit.t_stats_satellite;
BEGIN
    v_stats := pkg_nanoOrbit.stats_satellite('SAT-001');
    DBMS_OUTPUT.PUT_LINE('Fenêtres : ' || v_stats.nb_fenetres);
    DBMS_OUTPUT.PUT_LINE('Volume   : ' || v_stats.volume_total || ' Mo');
END;

-- 5. Statut global de la constellation
BEGIN DBMS_OUTPUT.PUT_LINE(pkg_nanoOrbit.statut_constellation); END;
```

---

## 6. Phase 4 — Exploitation avancée & Optimisation

> **TP associé** : `TP4 — Requêtes avancées` couvre les vues, CTEs (WITH ... AS), fonctions analytiques OVER (PARTITION BY / ORDER BY), sous-requêtes scalaires et corrélées, EXISTS/NOT EXISTS, MERGE INTO, et EXPLAIN PLAN. La Phase 4 du projet applique l'ensemble de ces techniques sur le schéma NanoOrbit.

### 6.1 Vues (simples et matérialisée)

#### V1 — `V_SATELLITES_OPERATIONNELS` (vue simple filtrée)

Retourne uniquement les satellites au statut 'Opérationnel' avec jointure sur ORBITE et comptage des instruments embarqués via `LEFT JOIN EMBARQUEMENT`. Inclut `GROUP BY` sur toutes les colonnes non agrégées.

```sql
-- Résultat :
-- SAT-001, NanoOrbit-Alpha, 3U, 20.0, Opérationnel, SSO, 550, 2
-- SAT-002, NanoOrbit-Beta,  3U, 20.0, Opérationnel, SSO, 550, 1
-- SAT-003, NanoOrbit-Gamma, 6U, 40.0, Opérationnel, SSO, 700, 2
```

#### V2 — `V_FENETRES_DETAIL` (vue jointure dénormalisée)

Consolide FENETRE_COM avec les noms du satellite, de la station et du centre de contrôle. Calcule la durée formatée en HH:MM:SS via `TO_CHAR(NUMTODSINTERVAL(f.duree, 'SECOND'), 'HH24:MI:SS')`. Utilise `LEFT JOIN` vers AFFECTATION_STATION et CENTRE_CONTROLE pour conserver les fenêtres sans centre associé.

#### V3 — `V_STATS_MISSIONS` (vue avec agrégats)

Par mission : nombre de satellites distincts, nombre de types d'orbite distincts représentés, volume total téléchargé (`NVL(SUM(...), 0)`). Utilise des `LEFT JOIN` en cascade pour couvrir les missions sans participation.

```sql
-- Résultat :
-- MSN-ARC-2023,  ArcticWatch 2023,  3, 1, 3820
-- MSN-DEF-2022,  DeforestAlert,     2, 2, 1250
-- MSN-COAST-2024, CoastGuard 2024,  2, 1, 1680
```

#### V4 — `MV_VOLUMES_MENSUELS` (vue matérialisée — REFRESH ON DEMAND)

Vue matérialisée agrégeant les volumes téléchargés par mois (`TRUNC(datetime_debut, 'MM')`), par centre de contrôle et par format de satellite. Filtre sur les fenêtres 'Réalisées' uniquement. Nécessite un `EXEC DBMS_MVIEW.REFRESH('MV_VOLUMES_MENSUELS')` pour être mise à jour.

L'index fonctionnel `IDX_FENETRE_MOIS` sur `TRUNC(datetime_debut, 'fmmm')` a été créé pour optimiser le regroupement mensuel utilisé dans cette vue.

```sql
-- Résultat après refresh :
-- 2024-01-01, NanoOrbit Paris HQ, 3U, 2140
-- 2024-01-01, NanoOrbit Paris HQ, 6U, 1680
```

### 6.2 CTE et sous-requêtes avancées

#### CTE simple — Top 3 satellites par volume (Ex. 5)

```sql
WITH stats_satellites AS (
    SELECT s.id_satellite, s.nom_satellite,
           COUNT(f.id_fenetre) AS nb_fenetres_realisees,
           NVL(SUM(f.volume_donnees), 0) AS volume_total
    FROM SATELLITE s LEFT JOIN FENETRE_COM f ON s.id_satellite = f.id_satellite
                                              AND f.statut = 'Réalisée'
    GROUP BY s.id_satellite, s.nom_satellite
)
SELECT * FROM (SELECT * FROM stats_satellites ORDER BY volume_total DESC)
WHERE ROWNUM <= 3;
-- SAT-003 (1680), SAT-001 (1250), SAT-002 (890)
```

#### CTE multiples — Analyse comparative par centre (Ex. 6)

Trois CTEs chaînées : `fenetres_mois` (jointure CENTRE→STATION→FENETRE), `stats_centre` (agrégats par centre), `stats_station` (station la plus active par centre avec `ROW_NUMBER() OVER (PARTITION BY id_centre ORDER BY COUNT(...) DESC)`). La combinaison illustre la puissance des CTEs pour décomposer des requêtes complexes.

#### CTE récursive — Hiérarchie Centre → Station → Fenêtre (Ex. 7)

CTE récursive `hierarchie` à 3 niveaux (CENTRE, STATION, FENETRE) avec `LPAD(' ', (niveau-1)*4)` pour l'indentation visuelle. Résultat :

```
NanoOrbit Paris HQ (CENTRE)
    Kiruna Arctic Station (STATION)
        Fenêtre 1 - 2024-01-15 09:14 (FENETRE)
        Fenêtre 3 - 2024-01-16 08:30 (FENETRE)
    Toulouse Ground Station (STATION)
        Fenêtre 2 - 2024-01-15 11:52 (FENETRE)
        ...
```

#### Sous-requête scalaire — Fenêtres au-dessus de la moyenne (Ex. 8)

Calcule la moyenne générale des volumes réalisés, puis filtre les fenêtres au-dessus de cette moyenne en calculant l'écart. Seule la fenêtre 3 (1680 Mo) dépasse la moyenne de 1273,33 Mo (écart : +406,67).

#### Sous-requête corrélée — Dernière fenêtre réalisée par satellite (Ex. 9)

```sql
WHERE f.datetime_debut = (
    SELECT MAX(f2.datetime_debut) FROM FENETRE_COM f2
    WHERE f2.id_satellite = s.id_satellite AND f2.statut = 'Réalisée'
)
```
La sous-requête est corrélée car elle dépend du satellite courant de la requête principale.

#### EXISTS / NOT EXISTS — Satellites et stations inactifs (Ex. 10)

- **NOT EXISTS** sur FENETRE_COM : identifie SAT-004 et SAT-005 (aucune fenêtre réalisée)
- **NOT EXISTS** sur la période Q1 2024 : identifie GS-SGP-01 (Singapore, en Maintenance — cas métier cohérent)

### 6.3 Fonctions analytiques OVER

#### ROW_NUMBER / RANK / DENSE_RANK (Ex. 11)

Classement global et par type d'orbite des satellites selon leur volume total téléchargé :

```sql
ROW_NUMBER() OVER (ORDER BY volume_total DESC) AS rn_global,
RANK()       OVER (PARTITION BY type_orbite ORDER BY volume_total DESC) AS rank_par_orbite
```

Illustre la différence entre RANK (sauts en cas d'ex-aequo) et DENSE_RANK (pas de saut).

#### LAG / LEAD (Ex. 12)

Pour chaque fenêtre de communication d'une station, compare le volume avec la fenêtre précédente (`LAG(volume_donnees) OVER (PARTITION BY code_station ORDER BY datetime_debut)`) et calcule l'évolution en pourcentage. Permet de détecter des tendances sur les communications d'une station.

#### SUM OVER — Volumes cumulés (Ex. 13)

```sql
SUM(volume_donnees) OVER (
    PARTITION BY id_centre ORDER BY datetime_debut
    ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
) AS moyenne_mobile_3
```

Produit un cumul chronologique des volumes par centre avec moyenne mobile sur les 3 dernières fenêtres.

#### Tableau de bord constellation (Ex. 14)

Requête finale combinant `RANK`, `SUM OVER`, `ROUND` et CTEs pour produire le rapport mensuel complet : rang du satellite par volume, part % du volume total de la constellation, cumul, comparaison à la moyenne.

### 6.4 MERGE INTO — Synchronisation

#### Synchronisation des statuts satellites depuis un système IoT externe (Ex. 15)

```sql
MERGE INTO SATELLITE s
USING (SELECT 'SAT-004' AS id, 'Opérationnel' AS statut FROM DUAL
       UNION ALL SELECT 'SAT-NEW', 'En veille' FROM DUAL) src
ON (s.id_satellite = src.id)
WHEN MATCHED THEN UPDATE SET s.statut = src.statut
WHEN NOT MATCHED THEN INSERT (...) VALUES (...);
```

Si le satellite existe → mise à jour du statut. Si nouveau (cas rare) → insertion avec statut 'En veille'.

#### Synchronisation des affectations de stations (Ex. 16)

Simule l'arrivée d'un fichier de configuration révisé : met à jour les dates d'affectation existantes et crée les nouvelles associations. Permet notamment d'ajouter CTR-003 (Singapour) et l'affecter à GS-SGP-01.

### 6.5 Index & EXPLAIN PLAN

#### Index stratégiques (Ex. 17)

Les index suivants ont été créés pour optimiser les requêtes critiques :

| Index | Table | Colonnes | Justification |
|---|---|---|---|
| `IDX_SATELLITE_ID_ORBITE` | SATELLITE | `(id_orbite)` | Accélère les jointures SATELLITE → ORBITE |
| `IDX_SATELLITE_STATUT` | SATELLITE | `(statut)` | Filtre rapide par statut opérationnel |
| `IDX_SATELLITE_STATUT_ORBITE` | SATELLITE | `(statut, id_orbite)` | Index composite pour double filtre |
| `IDX_STATION_STATUT` | STATION_SOL | `(statut)` | Planification de fenêtres (stations actives) |
| `IDX_MISSION_STATUT` | MISSION | `(statut_mission)` | Filtre missions actives / terminées |
| `IDX_FENETRE_CODE_STATION` | FENETRE_COM | `(code_station)` | Fenêtres par station |
| `IDX_FENETRE_ID_SATELLITE` | FENETRE_COM | `(id_satellite)` | Fenêtres par satellite |
| `IDX_FENETRE_DATETIME` | FENETRE_COM | `(datetime_debut)` | Tri chronologique et vérification chevauchement |
| `IDX_FENETRE_MOIS` | FENETRE_COM | `TRUNC(datetime_debut, 'fmmm')` | Index fonctionnel — regroupement mensuel (MV) |
| `IDX_HISTORIQUE_SAT_TS` | HISTORIQUE_STATUT | `(id_satellite, timestamp_)` | Chronologie des statuts par satellite |
| `IDX_EMB_REF_INSTRUMENT` | EMBARQUEMENT | `(ref_instrument)` | "Sur quels satellites est cet instrument ?" |
| `IDX_AFF_CODE_STATION` | AFFECTATION_STATION | `(code_station)` | "Quel centre supervise cette station ?" |
| `IDX_PART_ID_MISSION` | PARTICIPATION | `(id_mission)` | "Quels satellites participent à cette mission ?" |

#### Lecture d'un EXPLAIN PLAN (Ex. 18)

L'EXPLAIN PLAN de la requête de reporting mensuel (JOIN sur 4 tables + GROUP BY + agrégats) révèle des `TABLE ACCESS FULL` sur FENETRE_COM. L'ajout de l'index `IDX_FENETRE_MOIS` transforme ces accès en `INDEX RANGE SCAN`, réduisant le coût de plusieurs ordres de grandeur.

#### Mesure de l'impact d'un index invisible (Ex. 19)

Protocole de test :
1. `ALTER INDEX IDX_FENETRE_MOIS INVISIBLE` → Oracle ignore l'index
2. Relever le plan : `TABLE ACCESS FULL` (coût élevé)
3. `ALTER INDEX IDX_FENETRE_MOIS VISIBLE` → l'index est réactivé
4. Relever le plan : `INDEX RANGE SCAN` (coût réduit)

Permet de documenter précisément le gain apporté par chaque index sans le supprimer physiquement.

#### Rapport de pilotage intégral — Requête de synthèse finale

Requête combinant CTE, fonctions analytiques et vue matérialisée pour produire le tableau de bord opérationnel NanoOrbit :

- Rang des centres de contrôle par volume téléchargé (`RANK OVER`)
- Part % du volume total de la constellation (`SUM OVER` global)
- Évolution par rapport au mois précédent (`LAG OVER`)
- Statut de chaque satellite rattaché aux stations du centre

---

## 7. Règles métier complètes

### Règles structurelles (PK, FK, UNIQUE)

| Règle | Description | Implémentation |
|---|---|---|
| RG-S01 | Chaque satellite est identifié de manière unique par `id_satellite` | PK sur SATELLITE |
| RG-S02 | Un satellite est associé à une et une seule orbite | FK `fk_satellite_orbite` |
| RG-S03 | Un satellite peut embarquer plusieurs instruments | Table EMBARQUEMENT |
| RG-S04 | L'association satellite ↔ instrument porte `date_integration` et `etat_fonctionnement` | Table EMBARQUEMENT avec PK composite |
| RG-S05 | Un satellite participe à au moins une mission | Table PARTICIPATION |
| RG-O01 | Une orbite peut être associée à plusieurs satellites | FK dans SATELLITE |
| RG-O02 | Le couple `(altitude, inclinaison)` est unique pour chaque orbite | `UQ_ORBITE_ALT_INC` |
| RG-O03 | Une orbite peut exister sans satellite associé | FK `ON DELETE RESTRICT` côté SATELLITE |
| RG-I01 | Un instrument peut être embarqué sur plusieurs satellites | Table EMBARQUEMENT |
| RG-I02 | Un satellite peut embarquer plusieurs instruments | Table EMBARQUEMENT |
| RG-G01 | Chaque station au sol possède un identifiant unique et une localisation | PK + NOT NULL sur latitude/longitude |
| RG-G02 | Une station peut communiquer avec plusieurs satellites, et vice-versa | Table FENETRE_COM |
| RG-G04 | Une station peut être rattachée à plusieurs centres, et un centre supervise plusieurs stations | Table AFFECTATION_STATION |
| RG-F01 | Une fenêtre de communication implique obligatoirement un satellite et une station | FK NOT NULL dans FENETRE_COM |
| RG-M02 | Une mission peut impliquer plusieurs satellites | Table PARTICIPATION |
| RG-M03 | Un satellite peut participer à plusieurs missions | Table PARTICIPATION |

### Règles de contraintes simples (CHECK, NOT NULL)

| Règle | Description | Implémentation |
|---|---|---|
| RG-F04 | La durée d'une fenêtre est comprise entre 1 et 900 secondes | `CHECK (duree BETWEEN 1 AND 900)` |
| RG-F05 | Le volume de données est renseigné uniquement si la fenêtre est réalisée | `CHECK volume IS NULL OR volume >= 0` + Trigger T3 |
| RG-M01 | Une mission possède une date de début obligatoire et une date de fin facultative | `date_debut NOT NULL`, `date_fin NULL` |

### Règles procédurales (Triggers / PL/SQL)

| Règle | Description | Implémentation |
|---|---|---|
| RG-S06 | Un satellite Désorbité ne peut plus générer de fenêtres ni être assigné à une mission | Trigger T1 (FENETRE_COM) + Trigger T4 (PARTICIPATION) |
| RG-F02 | Deux fenêtres ne peuvent pas se chevaucher pour un même satellite | Trigger T2 |
| RG-F03 | Deux fenêtres ne peuvent pas se chevaucher pour une même station | Trigger T2 |
| RG-G03 | Une station en Maintenance ne peut pas planifier de nouvelles fenêtres | Trigger T1 |
| RG-I03 | Un instrument ne peut pas être embarqué simultanément sur deux satellites | PK composite dans EMBARQUEMENT |
| RG-M04 | Une mission terminée ne peut plus accueillir de nouveaux satellites | Trigger T4 |

---

## 8. Correspondance avec les TPs

| Phase du projet | TP associé | Thèmes couverts |
|---|---|---|
| **Phase 1** — Conception & Modélisation | `TP1 — Modélisation & VLS` (02/03/2026) | Méthode MERISE, dictionnaire des données, MCD, MLD, répartition multi-sites, classification des règles de gestion |
| **Phase 2 (DDL)** — Création du schéma | `TP2 — Schéma VLS & Triggers` (04/03/2026) | DDL Oracle (CREATE TABLE, contraintes, index), ordre de création par dépendances référentielles |
| **Phase 2 (DML)** — Insertion des données | `TP2 — Schéma VLS & Triggers` (04/03/2026) | INSERT INTO dans le bon ordre, COMMIT, respect du référentiel |
| **Phase 2 (Triggers)** — Règles procédurales | `TP2 — Schéma VLS & Triggers` (04/03/2026) | BEFORE/AFTER triggers, :NEW/:OLD, RAISE_APPLICATION_ERROR, cas d'erreur |
| **Phase 3 (Paliers 1-4)** — Blocs et curseurs | `TP3 — PL/SQL` (17/03/2026) | Blocs anonymes, %TYPE, %ROWTYPE, SELECT INTO, NVL, IF/CASE, boucles FOR, curseurs implicites/explicites/paramétrés |
| **Phase 3 (Palier 5)** — Procédures et fonctions | `TP3 — PL/SQL` (17/03/2026) | CREATE PROCEDURE/FUNCTION, paramètres IN/OUT, RAISE_APPLICATION_ERROR, gestion exceptions |
| **Phase 3 (Palier 6)** — Package | `TP3 — PL/SQL` (17/03/2026) | SPEC + BODY, types publics, constantes, encapsulation des opérations métier |
| **Phase 4 (Vues)** — CREATE VIEW | `TP4 — Requêtes avancées` (31/03/2026) | Vues simples, jointures dénormalisées, agrégats, vues matérialisées REFRESH ON DEMAND |
| **Phase 4 (CTE)** — WITH … AS | `TP4 — Requêtes avancées` (31/03/2026) | CTEs simples, multiples, récursives avec LPAD pour indentation |
| **Phase 4 (Sous-requêtes)** — Avancé | `TP4 — Requêtes avancées` (31/03/2026) | Sous-requêtes scalaires, corrélées, EXISTS / NOT EXISTS |
| **Phase 4 (Analytiques)** — OVER | `TP4 — Requêtes avancées` (31/03/2026) | ROW_NUMBER, RANK, DENSE_RANK, LAG, LEAD, SUM OVER, PARTITION BY |
| **Phase 4 (MERGE INTO)** | `TP4 — Requêtes avancées` (31/03/2026) | Synchronisation WHEN MATCHED / WHEN NOT MATCHED |
| **Phase 4 (Index & EXPLAIN PLAN)** | `TP4 — Requêtes avancées` (31/03/2026) | Index stratégiques, index fonctionnels, lecture d'EXPLAIN PLAN, index invisible |

---

*Projet réalisé dans le cadre du module ALTN83 — Bases de données réparties · EFREI Paris Panthéon-Assas · Cycle ingénieur 2025-2026*