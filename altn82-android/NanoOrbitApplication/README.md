# NanoOrbit Ground Control

Application Android de supervision de flotte CubeSat. Elle permet à des opérateurs sol de surveiller en temps réel l'état de satellites, de planifier et gérer des fenêtres de communication, de visualiser les stations sol sur une carte interactive et de suivre les missions scientifiques ou opérationnelles associées.

**API REST** : `https://nano-orbit.vlegall.fr`  
**Documentation API** : `/swagger-ui/index.html`  
**Module** : ALTN82 — Développement Mobile Android  
**Langage** : Kotlin 100% · Jetpack Compose · minSdk 26

---

## Table des matières

1. [Présentation du projet](#1-présentation-du-projet)
2. [Structure des fichiers](#2-structure-des-fichiers)
3. [Architecture MVVM](#3-architecture-mvvm)
4. [Flux de données réactif](#4-flux-de-données-réactif)
5. [Principes SOLID](#5-principes-solid)
6. [Couche Domain](#6-couche-domain)
7. [Couche Data — Réseau](#7-couche-data--réseau)
8. [Couche Data — Base de données Room](#8-couche-data--base-de-données-room)
9. [Couche Data — Repository](#9-couche-data--repository)
10. [Couche Présentation — ViewModels](#10-couche-présentation--viewmodels)
11. [Couche Présentation — Écrans](#11-couche-présentation--écrans)
12. [Couche Présentation — Composants](#12-couche-présentation--composants)
13. [Navigation](#13-navigation)
14. [Mode hors-ligne et synchronisation](#14-mode-hors-ligne-et-synchronisation)
15. [Validation des règles métier](#15-validation-des-règles-métier)
16. [Fonctionnalités bonus](#16-fonctionnalités-bonus)
17. [Injection de dépendances (AppContainer)](#17-injection-de-dépendances-appcontainer)
18. [Thème spatial](#18-thème-spatial)
19. [Stack technique](#19-stack-technique)
20. [Progression depuis les TPs](#20-progression-depuis-les-tps)
21. [Évaluation](#21-évaluation)

---

## 1. Présentation du projet

NanoOrbit Ground Control est une station de contrôle mobile pour une constellation de nano-satellites CubeSat. L'application se connecte à une API REST réelle et expose les données suivantes :

- **Satellites** : état opérationnel, format CubeSat, paramètres orbitaux, télémétrie (masse, batterie, durée de vie)
- **Fenêtres de communication** : créneaux planifiés ou réalisés entre un satellite et une station sol, avec durée en secondes et volume de données échangées
- **Stations sol** : antennes réparties sur le globe, avec coordonnées GPS, bande de fréquence et débit maximal
- **Instruments embarqués** : capteurs ou récepteurs montés sur les satellites
- **Missions** : programmes scientifiques ou opérationnels auxquels participent les satellites
- **Historique des statuts** : journal de tous les changements d'état d'un satellite

Le périmètre fonctionnel complet couvre : une liste filtrée de satellites, un écran de détail multi-sections, un planning de communication avec création de fenêtres validée, une carte interactive des stations sol, et trois fonctionnalités bonus (notifications, pull-to-refresh, favoris).

---

## 2. Structure des fichiers

```
app/src/main/java/fr/vlegall/nanoorbitapplication/
│
├── NanoOrbitApplication.kt          ← Application Android (init osmdroid, canal notif)
├── MainActivity.kt                  ← Point d'entrée, lance le NavHost
│
├── di/
│   └── AppContainer.kt             ← DI manuelle : construit et câble toutes les dépendances
│
├── navigation/
│   ├── Routes.kt                   ← Sealed class des routes + items BottomNav
│   └── NanoOrbitNavHost.kt         ← NavHost + NavigationBar
│
├── domain/
│   ├── model/
│   │   ├── Enums.kt                ← StatutSatellite, StatutFenetre, StatutStation, TypeOrbite, FormatCubeSat
│   │   ├── Satellite.kt            ← Satellite (complet) + SatelliteSummary (allégé) + Orbite
│   │   ├── FenetreCom.kt           ← FenetreCom + FenetreComCreateRequest + constantes RG-F04
│   │   ├── StationSol.kt           ← StationSol + canReceiveWindow
│   │   ├── Mission.kt              ← Mission
│   │   ├── Participation.kt        ← Participation (liaison satellite ↔ mission)
│   │   ├── Embarquement.kt         ← Embarquement (instrument sur satellite)
│   │   ├── HistoriqueStatut.kt     ← HistoriqueStatut
│   │   └── Dashboard.kt            ← Dashboard (statistiques globales)
│   └── repository/
│       └── NanoOrbitRepository.kt  ← Interface du repository (contrat SOLID-D)
│
├── data/
│   ├── remote/
│   │   ├── NanoOrbitApi.kt         ← Interface Retrofit (17 endpoints)
│   │   ├── RetrofitClient.kt       ← Factory Retrofit + ApiKeyInterceptor
│   │   └── dto/
│   │       └── ApiDtos.kt          ← Tous les DTOs de réponse et de requête
│   ├── local/
│   │   ├── NanoOrbitDatabase.kt    ← RoomDatabase (7 entités, version 4)
│   │   ├── FavoritesDataStore.kt   ← DataStore Preferences (favoris)
│   │   ├── converter/
│   │   │   └── DateConverter.kt    ← LocalDate/LocalDateTime ↔ String ISO-8601
│   │   ├── entity/
│   │   │   ├── SatelliteEntity.kt
│   │   │   ├── FenetreEntity.kt
│   │   │   ├── StationEntity.kt
│   │   │   ├── EmbarquementEntity.kt
│   │   │   ├── ParticipationEntity.kt
│   │   │   ├── HistoriqueStatutEntity.kt
│   │   │   └── PendingOperationEntity.kt
│   │   └── dao/
│   │       ├── SatelliteDao.kt
│   │       ├── FenetreDao.kt
│   │       ├── StationDao.kt
│   │       ├── EmbarquementDao.kt
│   │       ├── ParticipationDao.kt
│   │       ├── HistoriqueStatutDao.kt
│   │       └── PendingOperationDao.kt
│   ├── network/
│   │   └── NetworkConnectivityObserver.kt ← Observer de connectivité (callbackFlow)
│   └── repository/
│       └── NanoOrbitRepositoryImpl.kt     ← Implémentation Network-First + mappers
│
├── presentation/
│   ├── dashboard/
│   │   ├── DashboardViewModel.kt
│   │   └── DashboardScreen.kt
│   ├── detail/
│   │   ├── DetailViewModel.kt
│   │   └── DetailScreen.kt
│   ├── planning/
│   │   ├── PlanningViewModel.kt
│   │   └── PlanningScreen.kt
│   ├── map/
│   │   ├── MapViewModel.kt
│   │   └── MapScreen.kt
│   └── components/
│       ├── SatelliteCard.kt
│       ├── StatusBadge.kt
│       ├── FenetreCard.kt
│       ├── InstrumentItem.kt
│       └── OfflineBanner.kt
│
├── mock/
│   └── MockData.kt                 ← Données simulées pour @Preview uniquement
│
├── worker/
│   └── PassageNotificationWorker.kt ← CoroutineWorker (notifications périodiques)
│
└── ui/theme/
    ├── Color.kt                    ← Palette spatiale complète
    ├── Theme.kt                    ← darkColorScheme NanoOrbit
    └── Type.kt                     ← Typographie
```

---

## 3. Architecture MVVM

### Pourquoi MVVM ?

Le pattern **Model-View-ViewModel** résout le problème fondamental du développement Android : la View (Composable) est détruite et recréée à chaque rotation d'écran, changement de configuration ou retour arrière. Sans ViewModel, les données seraient perdues à chaque reconstruction.

- Le **Model** (couche domain + data) porte les données et la logique métier, sans aucune dépendance vers Android.
- Le **ViewModel** survit aux reconfigurations (`viewModelScope` est lié au cycle de vie de l'écran, pas de la vue). Il expose l'état via `StateFlow`.
- La **View** (Composable) observe les `StateFlow` et se recompose uniquement quand les données changent. Elle ne contient aucune logique, uniquement du rendu.

### Architecture multi-ViewModels

Ce projet utilise **quatre ViewModels distincts**, un par écran, plutôt qu'un ViewModel global unique. Cette décision architecturale offre plusieurs avantages concrets :

**Isolation des états** : naviguer vers le Planning et rafraîchir les fenêtres ne provoque aucune recomposition sur le Dashboard qui reste inchangé. Avec un ViewModel unique, tout changement d'état réémettrait sur tous les abonnés.

**Cycle de vie précis** : `DashboardViewModel` est créé quand l'utilisateur ouvre le Dashboard et détruit quand il le quitte définitivement. Si l'utilisateur revient, un nouveau ViewModel recharge les données. Avec un ViewModel global, les coroutines restent actives même quand l'écran n'est pas visible.

**Lisibilité** : chaque ViewModel est un fichier d'environ 100 lignes, autonome et facile à comprendre. Un ViewModel monolithique pour 4 écrans dépasserait 400 lignes avec des interdépendances difficiles à tracer.

**Testabilité** : on peut instancier `PlanningViewModel` seul en injectant un mock du repository, sans avoir à construire les dépendances des 3 autres ViewModels.

### Factory pattern

Chaque ViewModel expose une `Factory` statique via `viewModelFactory {}` pour s'injecter depuis le `Composable` sans utiliser de framework d'injection :

```kotlin
companion object {
    fun provideFactory(app: NanoOrbitApplication): ViewModelProvider.Factory =
        viewModelFactory {
            initializer { DashboardViewModel(app.container.repository) }
        }
}
```

Le Composable récupère l'application depuis `LocalContext` :

```kotlin
viewModel(factory = DashboardViewModel.provideFactory(
    LocalContext.current.applicationContext as NanoOrbitApplication
))
```

---

## 4. Flux de données réactif

### De la source vers l'interface

Le flux complet d'une donnée depuis l'API jusqu'au pixel rendu à l'écran suit ce chemin :

```
NanoOrbitApi (suspend fun) 
    ↓ coroutine IO
NanoOrbitRepositoryImpl 
    ↓ flow { emit() }
NanoOrbitRepository (Flow<T>)
    ↓ stateIn(viewModelScope)
ViewModel (StateFlow<T>)
    ↓ collectAsStateWithLifecycle()
Composable (State<T>)
    ↓ recomposition automatique
UI
```

### StateFlow vs LiveData

`StateFlow` est préféré à `LiveData` pour plusieurs raisons :
- Il est 100% Kotlin, sans dépendance vers `android.arch.lifecycle`.
- Il émet toujours une valeur initiale (utile pour afficher un état vide avant le premier chargement).
- Il est intégré nativement dans le système de coroutines.
- `collectAsStateWithLifecycle()` arrête la collecte quand le Composable n'est pas visible (économie de ressources).

### SharingStarted.WhileSubscribed(5_000)

Tous les `StateFlow` utilisent `SharingStarted.WhileSubscribed(5_000)`. Ce paramètre signifie : "arrête d'écouter le Flow source si aucun abonné ne collecte depuis 5 secondes". En pratique :

- L'utilisateur tourne son téléphone → le Composable disparaît 100ms → le StateFlow reste actif (évite un reload inutile).
- L'utilisateur navigue vers un autre écran → après 5 secondes sans abonné → le Flow Room se ferme et libère les ressources.

### combine() pour le filtrage

`DashboardViewModel` agrège quatre sources réactives indépendantes en une liste filtrée unique :

```kotlin
val filteredSatellites = combine(
    _satellites,       // liste brute depuis Room
    searchQuery,       // texte saisi dans le champ de recherche
    favoriteIds,       // IDs favoris depuis DataStore
    showOnlyFavorites  // état du chip "Favoris"
) { list, query, favIds, onlyFav ->
    var result = if (query.isBlank()) list
                 else list.filter { sat ->
                     sat.nomSatellite.contains(query, ignoreCase = true) ||
                     sat.idSatellite.contains(query, ignoreCase = true) ||
                     sat.orbite?.typeOrbite?.label?.contains(query, ignoreCase = true) == true
                 }
    if (onlyFav) result = result.filter { it.idSatellite in favIds }
    result
}
```

Cette approche garantit que le filtrage se passe **uniquement dans le ViewModel**, jamais dans le Composable. Chaque fois qu'une des quatre sources émet, `combine()` recalcule la liste.

### flatMapLatest() pour les filtres par statut

Le filtre par statut est légèrement différent car il change la requête vers le repository :

```kotlin
private val _satellites = selectedStatut
    .flatMapLatest { statut -> repository.getSatellitesStream(statut) }
    .stateIn(...)
```

`flatMapLatest` annule le Flow précédent et en démarre un nouveau à chaque changement de `selectedStatut`. Cela évite d'avoir plusieurs Flows actifs simultanément.

---

## 5. Principes SOLID

### S — Single Responsibility Principle

Chaque classe dans ce projet a une seule raison d'évoluer :

| Classe | Responsabilité unique |
|--------|----------------------|
| `NanoOrbitRepositoryImpl` | Accès aux données (réseau + cache + favoris) |
| `DashboardViewModel` | État et logique de l'écran Dashboard |
| `PlanningViewModel` | État et validation des fenêtres de communication |
| `SatelliteCard` | Rendu visuel d'une carte satellite |
| `FavoritesDataStore` | Persistance des IDs favoris via DataStore |
| `NetworkConnectivityObserver` | Observation de l'état réseau |
| `PassageNotificationWorker` | Envoi de notifications de passages |
| `DateConverter` | Conversion types java.time ↔ String pour Room |
| `ApiKeyInterceptor` | Injection du header d'authentification sur chaque requête |

Si la règle d'affichage d'une `SatelliteCard` change, on modifie `SatelliteCard.kt` seulement. Si la stratégie de cache change, on modifie `NanoOrbitRepositoryImpl.kt` seulement.

### O — Open/Closed Principle

Les enums `StatutSatellite`, `StatutFenetre`, `StatutStation`, `TypeOrbite` et `FormatCubeSat` sont **fermées à la modification** (leurs valeurs sont fixées) mais **ouvertes à l'extension** grâce à `fromLabel()` :

```kotlin
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
```

Le code qui consomme ces enums (mapping DTO→domaine, `when()` dans les Composables) ne change pas si l'API renvoie une nouvelle valeur non connue — `fromLabel()` retourne la valeur par défaut.

De même, le `when()` dans `StatusBadge` est exhaustif : le compilateur force à traiter tous les cas. Ajouter un cinquième statut produit une erreur de compilation plutôt qu'un comportement silencieusement incorrect.

### L — Liskov Substitution Principle

`NanoOrbitRepositoryImpl` implémente **intégralement** `NanoOrbitRepository`. Tout code qui utilise `NanoOrbitRepository` peut recevoir `NanoOrbitRepositoryImpl` ou une implémentation mock sans changer de comportement.

Concrètement, les ViewModels ne savent pas qu'ils parlent à une implémentation réelle. En test, on peut injecter un `FakeNanoOrbitRepository` qui retourne des données fixes sans réseau ni base de données.

### I — Interface Segregation Principle

L'interface `NanoOrbitRepository` est le seul contrat que les ViewModels doivent connaître. Elle regroupe toutes les opérations de données de l'application. Les ViewModels ne dépendent jamais de `NanoOrbitRepositoryImpl`, `NanoOrbitApi`, ou `SatelliteDao` directement.

Cela signifie que si demain on remplace Retrofit par Ktor, ou Room par SQLDelight, aucun ViewModel n'a besoin d'être modifié.

### D — Dependency Inversion Principle

Les modules de haut niveau (ViewModels) ne dépendent pas des modules de bas niveau (implémentation concrète du repository). Tous deux dépendent d'une abstraction (`NanoOrbitRepository`).

```kotlin
// AppContainer.kt — le seul endroit qui connaît l'implémentation concrète
val repository: NanoOrbitRepository = NanoOrbitRepositoryImpl(
    api = api,
    satelliteDao = database.satelliteDao(),
    fenetreDao = database.fenetreDao(),
    pendingOperationDao = database.pendingOperationDao(),
    stationDao = database.stationDao(),
    embarquementDao = database.embarquementDao(),
    participationDao = database.participationDao(),
    historiqueStatutDao = database.historiqueStatutDao(),
    favoritesDataStore = favoritesDataStore
)
```

```kotlin
// DashboardViewModel.kt — dépend uniquement de l'abstraction
class DashboardViewModel(private val repository: NanoOrbitRepository) : ViewModel()
```

---

## 6. Couche Domain

La couche domain ne contient **aucune dépendance Android**. Elle est constituée de `data class` Kotlin pures et d'une interface. Ce choix permet de la tester unitairement sur JVM sans émulateur.

### Modèles métier

**`Satellite`** — Représentation complète d'un satellite. Contient tous les champs de télémétrie (masse, capacité batterie, durée de vie prévue) et une référence optionnelle à son `Orbite`. La propriété calculée `canScheduleWindow` encapsule la règle métier RG-S06 directement dans le modèle.

**`SatelliteSummary`** — Version allégée utilisée dans la liste Dashboard. Elle ne contient pas les champs de télémétrie (masse, batterie) qui ne sont nécessaires que sur l'écran de détail. Ce découpage évite de charger des données inutiles pour la liste.

**`Orbite`** — Paramètres orbitaux : type (SSO/LEO/MEO/GEO), altitude en km, inclinaison en degrés, période orbitale, excentricité et zone de couverture. L'orbite est dénormalisée dans le cache Room pour éviter une jointure à chaque affichage de la liste.

**`FenetreCom`** — Fenêtre de communication avec son satellite, sa station, sa durée en secondes, son élévation maximale et son volume de données échangées (nullable si non encore réalisée). Le `companion object` porte les constantes de validation `DUREE_MIN_SECONDES = 1` et `DUREE_MAX_SECONDES = 900`.

**`FenetreComCreateRequest`** — DTO de création utilisé entre le ViewModel et le repository. Il sépare les données nécessaires à la création (satellite, station, début, durée, élévation) du modèle complet retourné par l'API.

**`StationSol`** — Station au sol avec latitude et longitude pour les marqueurs osmdroid, bande de fréquence, débit maximal en Mbps et statut. La propriété `canReceiveWindow` encapsule RG-G03.

**`Mission`** — Mission scientifique ou opérationnelle avec objectif, dates de début et fin, zone géographique cible.

**`Participation`** — Table de liaison entre un satellite et une mission, avec le rôle spécifique du satellite dans cette mission.

**`Embarquement`** — Instrument embarqué sur un satellite : type (caméra, radar, récepteur AIS…), modèle, date d'intégration et état de fonctionnement (Nominal / Dégradé / Hors service).

**`HistoriqueStatut`** — Entrée d'historique horodatée d'un changement de statut. Alimentée côté serveur à chaque modification.

**`Dashboard`** — Agrégat de statistiques globales retourné par l'API : nombre de satellites par statut, nombre de fenêtres planifiées et réalisées, nombre de missions par statut.

### Interface NanoOrbitRepository

L'interface expose 20 méthodes couvrant tout le périmètre fonctionnel. Les méthodes de lecture intensive (listes de satellites, fenêtres, stations) retournent des `Flow<List<T>>` pour permettre la mise à jour réactive de l'interface dès que le cache Room change. Les opérations ponctuelles (chargement d'un détail, création, suppression) sont des `suspend fun` qui retournent `Result<T>`.

```kotlin
// Flux réactifs — Room émet à chaque modification du cache
fun getSatellitesStream(statut: StatutSatellite? = null): Flow<List<SatelliteSummary>>
fun getFenetresStream(statut: String? = null, ...): Flow<List<FenetreCom>>
fun getStationsStream(): Flow<List<StationSol>>

// Opérations ponctuelles
suspend fun getSatelliteById(id: String): Result<Satellite>
suspend fun createFenetre(request: FenetreComCreateRequest): Result<FenetreCom>
suspend fun signalerAnomalie(id: String, description: String): Result<Unit>

// Cache et favoris
fun getLastSyncTimestamp(): Flow<Long?>
suspend fun syncPendingOperations(): Result<Int>
fun getFavoriteIds(): Flow<Set<String>>
suspend fun toggleFavorite(id: String)
```

---

## 7. Couche Data — Réseau

### Retrofit et NanoOrbitApi

`NanoOrbitApi` est une interface Kotlin annotée avec les annotations Retrofit. Chaque méthode correspond exactement à un endpoint du Swagger. Retrofit génère automatiquement l'implémentation à l'exécution via `create(NanoOrbitApi::class.java)`.

Les 17 endpoints couvrent :

| Ressource | GET liste | GET détail | POST création | PATCH modif | DELETE |
|-----------|-----------|------------|---------------|-------------|--------|
| Dashboard | ✓ | — | — | — | — |
| Satellites | ✓ (avec `?statut`) | ✓ (`/{id}`) | — | ✓ (`/{id}/statut`) | — |
| Anomalies | — | — | ✓ (`/{id}/anomalies`) | — | — |
| Instruments | ✓ (`/{id}/instruments`) | — | — | — | — |
| Missions | ✓ (`/{id}/missions`) | — | — | — | — |
| Historique | ✓ (`/{id}/historique-statut`) | — | — | — | — |
| Fenêtres satellite | ✓ (`/{id}/fenetres`) | — | — | — | — |
| Fenêtres | ✓ (filtres multiples) | ✓ (`/{id}`) | ✓ | ✓ (`/{id}/realiser`) | ✓ |
| Stations | ✓ (avec `?statut`) | ✓ (`/{codeStation}`) | — | — | — |
| Fenêtres station | ✓ (`/{codeStation}/fenetres`) | — | — | — | — |

### RetrofitClient et sécurité

`RetrofitClient` est un objet Kotlin (`object`) qui expose une méthode `create()` retournant une instance de `NanoOrbitApi`. La clé API n'est jamais écrite dans le code source. Elle est stockée dans `local.properties` (ignoré par Git) et injectée au moment du build via `BuildConfig.API_KEY`.

`ApiKeyInterceptor` est un `Interceptor` OkHttp qui ajoute le header `X-API-Key` sur **chaque requête sortante** automatiquement. Le logging HTTP est activé uniquement en mode debug (`BuildConfig.DEBUG`) pour éviter de loguer des données sensibles en production.

Les timeouts (30 secondes en connexion, lecture et écriture) sont configurés pour tenir compte des conditions réseau mobiles dégradées.

### DTOs et séparation des modèles

Les classes `*Dto` dans `data/remote/dto/ApiDtos.kt` sont de simples conteneurs JSON, tous leurs champs sont `nullable`. Cette nullabilité est intentionnelle : l'API pourrait renvoyer des champs manquants dans certaines versions ou pour certaines configurations.

La séparation DTO ↔ modèle domaine est fondamentale. Le modèle domaine `Satellite` a `val statut: StatutSatellite` (type fort, non-nullable). Le DTO `SatelliteDto` a `val statutOperationnel: String?` (String nullable). Le mapping dans le repository fait la conversion et gère les cas `null` avec des valeurs par défaut.

Cette séparation protège le code métier : si l'API renomme un champ, seul le DTO et le mapper changent. Le reste de l'application est inchangé.

---

## 8. Couche Data — Base de données Room

### Pourquoi 7 entités ?

Le sujet ALTN82 demandait un minimum de 2 entités Room (`SatelliteEntity` et `FenetreEntity`). Ce projet en implémente 7 pour permettre un fonctionnement offline complet sur tous les écrans, pas seulement la liste des satellites.

| Entité | Table SQLite | Rôle dans l'application |
|--------|-------------|------------------------|
| `SatelliteEntity` | `satellites` | Cache de la liste Dashboard et des filtres |
| `FenetreEntity` | `fenetres_com` | Cache du planning, filtrable par station |
| `StationEntity` | `stations` | Cache des marqueurs de la carte OSM |
| `EmbarquementEntity` | `embarquements` | Cache des instruments dans le détail satellite |
| `ParticipationEntity` | `participations` | Cache des missions dans le détail satellite |
| `HistoriqueStatutEntity` | `historique_statut` | Cache de l'historique de statuts |
| `PendingOperationEntity` | `pending_operations` | File FIFO des opérations hors-ligne |

### SatelliteEntity — dénormalisation de l'orbite

L'orbite est dénormalisée directement dans `SatelliteEntity` sous forme de champs plats (`idOrbite`, `typeOrbite`, `altitude`, `inclinaison`…) plutôt que via une table séparée. Cette décision de conception évite une jointure SQL à chaque affichage de la liste des satellites. La liste peut afficher "SSO · 500 km" sans requête supplémentaire.

### PendingOperationEntity — système FIFO hors-ligne

`PendingOperationEntity` mérite une attention particulière. Elle stocke les opérations d'écriture (création de fenêtre, signalement d'anomalie, changement de statut) effectuées quand l'API est inaccessible.

```kotlin
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,        // "CREATE_FENETRE", "DELETE_FENETRE", etc.
    val payload: String,     // JSON sérialisé des paramètres
    val createdAt: Long = System.currentTimeMillis()
)
```

Le `payload` est un objet JSON sérialisé via Gson. Par exemple, pour une création de fenêtre :

```json
{
  "localId": "-1714000000000",
  "idSatellite": "SAT-002",
  "codeStation": "GS-PAR",
  "datetimeDebut": "2026-04-24T14:30:00",
  "duree": "600",
  "elevationMax": "45.0"
}
```

Le `localId` négatif est un identifiant temporaire attribué à la fenêtre créée localement dans Room, pour qu'elle apparaisse immédiatement dans le planning. Quand la synchronisation réussit, cet ID temporaire est remplacé par l'ID réel retourné par le serveur.

### DateConverter

Room ne sait pas nativement persister les types `java.time.LocalDate` et `java.time.LocalDateTime`. `DateConverter` les convertit en String ISO-8601 (`"2026-04-23"` et `"2026-04-23T14:30:00"`). Avec minSdk 26, `java.time` est disponible nativement sans bibliothèque de desugaring supplémentaire.

Le parsing utilise `runCatching { }.getOrNull()` pour ne pas propager d'exception si la chaîne est malformée — la date devient `null` et l'UI affiche "—" à la place.

### DAOs

Chaque entité a son DAO. Les méthodes qui alimentent les `StateFlow` des ViewModels retournent des `Flow<List<T>>` (Room les émet automatiquement à chaque modification de la table). Les autres méthodes (insertion, suppression, lecture ponctuelle) sont des `suspend fun`.

Toutes les insertions utilisent `OnConflictStrategy.REPLACE` (upsert) : si un satellite existe déjà en cache, il est mis à jour avec les nouvelles données au lieu de provoquer une erreur.

---

## 9. Couche Data — Repository

`NanoOrbitRepositoryImpl` est la pièce centrale de la couche data. Il implémente `NanoOrbitRepository` en orchestrant les appels réseau, la mise à jour du cache Room, la gestion du mode hors-ligne et la synchronisation différée.

### Stratégie Network-First

Pour toutes les lectures de données principales, la stratégie est :

1. **Appel API** : `api.getSatellites()` est tenté en premier.
2. **Succès** → les données sont insérées/remplacées dans Room via `satelliteDao.insertAll()`. `_isOffline.value = false`. `_lastSyncTimestamp` est mis à jour.
3. **Échec** (toute exception : timeout, pas de réseau, erreur 5xx) → `_isOffline.value = true`. Aucune exception n'est propagée.
4. **Émission depuis Room** : qu'il y ait eu un succès ou un échec réseau, le Flow Room émet les données disponibles (fraîches ou en cache). Le ViewModel reçoit toujours des données.

Ce comportement est implémenté dans tous les streams :

```kotlin
override fun getSatellitesStream(statut: StatutSatellite?): Flow<List<SatelliteSummary>> = flow {
    try {
        val fresh = api.getSatellites(statut?.label)
        satelliteDao.insertAll(fresh.map { it.toEntity() })
        _isOffline.value = false
        _lastSyncTimestamp.value = System.currentTimeMillis()
    } catch (e: Exception) {
        _isOffline.value = true
    }
    val dbFlow = if (statut == null) satelliteDao.observeAll()
                 else satelliteDao.observeByStatut(statut.label)
    dbFlow.map { it.map { e -> e.toSummaryDomain() } }.collect { emit(it) }
}
```

### safeApiCall — helper de gestion d'erreur

Pour les opérations ponctuelles (chargement d'un détail, getStations…), un helper factorise la gestion de l'état hors-ligne :

```kotlin
private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> =
    runCatching { block() }.also { result ->
        result.onSuccess { _isOffline.value = false }
        result.onFailure { _isOffline.value = true }
    }
```

### forceRefresh

`forceRefresh()` recharge en séquence satellites, fenêtres et stations depuis l'API, vide les tables Room correspondantes (`clearAll()`) et les réinsère. Cette méthode est appelée par les ViewModels en réponse à un pull-to-refresh ou au démarrage de l'écran.

Avant de rafraîchir, `forceRefresh()` appelle `syncPendingOperations()` pour s'assurer que les opérations offline en attente sont envoyées à l'API avant de recevoir les données fraîches. Cela garantit la cohérence : on ne reçoit pas des données qui ignorent des modifications locales non encore synchronisées.

### Mappers

Le repository contient environ 20 fonctions d'extension privées qui convertissent les données entre les trois représentations :

- **DTO → Entity** : `SatelliteSummaryDto.toEntity()` — pour stocker dans Room après un appel réseau réussi.
- **DTO → Domain** : `FenetreComDto.toDomain()` — pour retourner directement au ViewModel sans passer par Room.
- **Entity → Domain** : `SatelliteEntity.toSummaryDomain()` — pour exposer les données Room comme des modèles domaine.
- **Domain → Entity** : `FenetreCom.toEntity()` — pour persister dans Room une entité construite localement (mode hors-ligne).

---

## 10. Couche Présentation — ViewModels

### DashboardViewModel

Gère l'écran principal avec :

- **`_satellites`** : chargé via `selectedStatut.flatMapLatest { repository.getSatellitesStream(it) }`. Le `flatMapLatest` annule le flow précédent et en démarre un nouveau à chaque changement de filtre statut.
- **`filteredSatellites`** : résultat de `combine(_satellites, searchQuery, favoriteIds, showOnlyFavorites)`. C'est le StateFlow que la LazyColumn consomme.
- **`dashboard`** : statistiques globales (`getDashboard()`) chargées en parallèle au démarrage.
- **`lastSyncTimestamp`** : exposé depuis `repository.getLastSyncTimestamp()` pour l'`OfflineBanner`.
- **`favoriteIds`** : flux DataStore branché directement depuis le repository.
- **`showOnlyFavorites`** : `MutableStateFlow<Boolean>` togglé par le chip "Favoris".

Fonctions publiques : `loadSatellites()`, `onSearchQueryChange()`, `onStatutFilterChange()`, `refreshSatellites()`, `toggleFavorite()`, `onShowOnlyFavoritesChange()`, `dismissError()`.

### DetailViewModel

Charge en parallèle cinq sources de données pour un satellite donné. `loadSatellite(id)` lance cinq coroutines concurrentes avec `viewModelScope.launch {}` imbriqués pour minimiser le temps d'attente :

```kotlin
fun loadSatellite(id: String) {
    viewModelScope.launch {
        _isLoading.value = true
        launch { repository.getSatelliteById(id).onSuccess { _satellite.value = it } }
        launch { repository.getInstruments(id).onSuccess { _instruments.value = it } }
        launch { repository.getMissions(id).onSuccess { _missions.value = it } }
        launch { repository.getHistoriqueStatut(id).onSuccess { _historique.value = it } }
        launch { repository.getFenetresBySatellite(id).onSuccess { _fenetres.value = it } }
        _isLoading.value = false
    }
}
```

Gère aussi `signalerAnomalie()` (validation min 5 chars côté ViewModel) et `updateStatut()`.

### PlanningViewModel

Combine les fenêtres et le filtre station :

```kotlin
val fenetres = combine(
    repository.getFenetresStream(),
    selectedStation
) { list, station ->
    val sorted = list.sortedBy { it.datetimeDebut }
    if (station != null) sorted.filter { it.codeStation == station } else sorted
}
```

La méthode `createFenetre()` effectue trois validations avant l'appel API :
1. Durée dans [1, 900] — RG-F04
2. Station non en Maintenance — RG-G03
3. Satellite non Désorbité via `satellite.canScheduleWindow` — RG-S06

Si l'API retourne une `OfflineException`, la fenêtre est tout de même considérée comme créée (`_createSuccess = true`) mais `_pendingSync = true` informe l'utilisateur qu'elle sera synchronisée plus tard.

### MapViewModel

Expose les stations depuis `repository.getStationsStream()` (Network-First + fallback Room) et la position GPS de l'utilisateur (`_userLocation`). Simple et léger : la complexité cartographique est gérée dans le Composable `MapScreen` avec osmdroid.

---

## 11. Couche Présentation — Écrans

### DashboardScreen

Point d'entrée de l'application. Affiche la liste complète des satellites de la constellation.

**Structure** : `Scaffold` avec `TopAppBar` (titre + icône refresh) → `Column` → `OfflineBanner` → `OutlinedTextField` (recherche) → `LazyRow` (chips de filtre) → compteur de résultats → contenu principal.

**États gérés** :
- Chargement initial (`isLoading && satellites.isEmpty()`) → `CircularProgressIndicator` centré.
- Erreur sans données (`errorMessage != null && satellites.isEmpty()`) → message d'erreur + bouton "Réessayer".
- Liste disponible → `PullToRefreshBox` wrappant la `LazyColumn`.
- Liste vide après filtre → item "Aucun satellite trouvé".

**`LazyColumn` avec `key`** : `items(filteredSatellites, key = { it.idSatellite })` permet à Compose d'identifier chaque item stable. Si un satellite change de statut, seule sa carte est recomposée, pas toute la liste.

**Satellites Désorbités** : la carte est rendue avec `alpha(0.5f)` et `clickable(enabled = false)`. L'utilisateur voit le satellite mais ne peut pas interagir avec lui, ce qui reflète la règle métier RG-S06.

### DetailScreen

Écran de détail multi-sections, chargé via `LaunchedEffect(satelliteId)` qui appelle `viewModel.loadSatellite(satelliteId)` à chaque changement d'ID.

**Six sections** affichées dans une `LazyColumn` :

1. **Statut** : `StatusBadge` coloré, format CubeSat (`1U/3U/6U/12U`), type d'orbite, altitude (km), inclinaison (degrés).
2. **Télémétrie** : masse (kg), durée de vie prévue (ans), durée restante estimée calculée via `ChronoUnit.YEARS.between(dateLancement, LocalDate.now())` avec `.coerceAtLeast(0)` pour éviter les valeurs négatives, capacité batterie avec `LinearProgressIndicator` branchée sur `cap / 100.0`.
3. **Instruments embarqués** : liste d'`InstrumentItem` avec bordure colorée (vert Nominal / ambre Dégradé / rouge Hors service).
4. **Missions actives** : liste des `Participation` avec nom de mission, rôle du satellite et commentaire éventuel.
5. **Historique des statuts** : liste de `HistoriqueStatutRow` avec dot coloré et timestamp formaté `dd/MM/yyyy HH:mm`.
6. **Fenêtres récentes** : les 5 dernières fenêtres du satellite (`fenetres.take(5)`).

**Bouton "Signaler une anomalie"** : désactivé si le satellite est Désorbité. Ouvre un `AlertDialog` avec `OutlinedTextField`, compteur de caractères `${text.length}/1000`, validation visuelle `isError` si moins de 5 caractères saisis.

### PlanningScreen

Écran de gestion des fenêtres de communication.

**StatChips** : 4 cartes d'indicateurs en `Row` avec `weight(1f)` chacune. `TextOverflow.Ellipsis` et `maxLines = 1` garantissent que le layout ne déborde pas même avec des grandes valeurs.

**Filtre stations** : `LazyRow` de `FilterChip`. Cliquer sur une station filtre les fenêtres affichées ; le chip "Toutes les stations" (`selectedStation == null`) réinitialise le filtre.

**FAB "Planifier"** : `ExtendedFloatingActionButton` qui ouvre `PlanifierFenetreDialog`.

**`PlanifierFenetreDialog`** : dialogue de création complet avec :
- `DropdownMenu` pour le satellite (uniquement les satellites avec `canScheduleWindow == true` présélectionnés)
- `DropdownMenu` pour la station (uniquement les stations avec `canReceiveWindow == true` présélectionnées)
- `OutlinedTextField` pour la date/heure au format `yyyy-MM-dd HH:mm`
- `OutlinedTextField` pour la durée (validation [1-900])
- `OutlinedTextField` pour l'élévation maximale (validation [0-90°])
- Affichage de l'erreur locale dans le dialogue avant l'envoi

**Snackbar** : un seul `SnackbarHostState` partagé pour les confirmations et les erreurs. Si la création réussit en mode hors-ligne, le message mentionne explicitement la synchronisation différée.

### MapScreen

Écran cartographique utilisant osmdroid via `AndroidView`.

**Initialisation de la carte** dans le `factory` de l'`AndroidView` :
- Source de tuiles : MAPNIK (OpenStreetMap standard)
- Multitouch activé
- Zoom min 3.0 / max 18.0
- `isHorizontalMapRepetitionEnabled = false` + `setScrollableAreaLimitLatitude/Longitude` : l'utilisateur ne peut pas faire défiler la carte pour voir le monde dupliqué
- `MyLocationNewOverlay` ajouté immédiatement pour avoir la position GPS disponible

**Marqueurs dynamiques** : à chaque changement de la liste `stations` (via `LaunchedEffect(stations, mapView)`), les marqueurs précédents sont supprimés et recréés. Chaque marqueur est un `Bitmap` circulaire généré par `createColoredMarkerBitmap()` :

```kotlin
private fun createColoredMarkerBitmap(context: Context, color: Int): BitmapDrawable {
    val size = (44 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawCircle(size/2f, size/2f, radius, paintFill)   // cercle coloré
    canvas.drawCircle(size/2f, size/2f, radius, paintStroke) // contour blanc
    return BitmapDrawable(context.resources, bitmap)
}
```

**Couleurs** : Actif → `#69F0AE` (vert), Maintenance → `#FFB300` (ambre), Inactif → `#546E7A` (gris).

**Infobulle** : `marker.title = station.nomStation`, `marker.subDescription` contient bande de fréquence, débit max, distance depuis la position GPS (Haversine via `Location.distanceBetween()`) et statut.

**`DisposableEffect`** : `mapView?.onDetach()` est appelé quand le Composable est supprimé de la composition pour libérer les ressources osmdroid correctement.

---

## 12. Couche Présentation — Composants

### SatelliteCard

Carte d'un satellite dans la liste Dashboard. Construite autour d'un `Card` avec `BorderStroke` coloré selon le statut.

- **Alpha** : `Modifier.alpha(if (isDesorbite) 0.5f else 1f)` — visuellement estompé si Désorbité.
- **Clics** : `clickable(enabled = !isDesorbite, onClick = onClick)` — non-cliquable si Désorbité.
- **Icône satellite** : teintée avec la couleur du statut.
- **Étoile favoris** : `IconButton` avec `Icons.Filled.Star` (jaune `#FFD600`) si favori, `Icons.Outlined.Star` (gris semi-transparent) sinon. Appel direct à `onFavoriteToggle()`.
- **Badge statut** : `StatusBadge` en fin de ligne.
- **Tag DÉSORBITÉ** : texte 9sp en monospace gris à côté du nom si applicable.

### StatusBadge

`Surface` arrondie (`RoundedCornerShape(50)`) avec couleur de fond à 15% d'opacité et texte en couleur pleine. Le `when()` exhaustif sur `StatutSatellite` garantit qu'aucun statut n'est oublié.

La fonction d'extension `StatutSatellite.toColor()` est accessible depuis n'importe quel Composable, permettant une cohérence totale des couleurs dans toute l'application.

### FenetreCard

Carte d'une fenêtre de communication. Affiche la date/heure de début en monospace, le badge de statut coloré, et trois colonnes d'informations (satellite, station, durée/élévation/volume). La fonction `Int.formatDuree()` convertit une durée en secondes en `"Xm Ys"`.

### InstrumentItem

Card d'instrument avec icône `Memory` teintée selon l'état de fonctionnement. La fonction d'extension `String.toEtatColor()` normalise les variantes textuelles (`"dégradé"` et `"degrade"`) au même résultat.

### OfflineBanner

`AnimatedVisibility(visible = isOffline)` wrappant une `Box` fond ambre. Affiche l'icône `CloudOff` et le texte "Mode hors-ligne · Mis à jour il y a Xm". Le calcul de l'âge du cache utilise `TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastSyncTimestamp)`.

---

## 13. Navigation

### Routes

`Routes` est une `sealed class` avec quatre `data object` imbriqués, chacun portant sa route sous forme de `String`.

```kotlin
sealed class Routes(val route: String) {
    data object Dashboard : Routes("dashboard")
    data object Planning  : Routes("planning")
    data object Map       : Routes("map")
    data object Detail    : Routes("detail/{satelliteId}") {
        fun createRoute(satelliteId: String) = "detail/$satelliteId"
        const val ARG_SATELLITE_ID = "satelliteId"
    }
}
```

`createRoute(id)` génère la route concrète `"detail/SAT-001"` à partir de l'ID. Le companion object liste les routes de la `NavigationBar` et les routes sans barre de navigation.

### NanoOrbitNavHost

`NavHost` avec `rememberNavController()`. La `NavigationBar` (3 onglets) est affichée via `Scaffold.bottomBar`. Elle est masquée sur `DetailScreen` en vérifiant si la route courante commence par `"detail/"` :

```kotlin
val showBottomNav = currentDestination?.route?.let { route ->
    !Routes.routesWithoutBottomNav.any { route.startsWith("detail/") }
} ?: true
```

La navigation vers `DetailScreen` utilise `navController.navigate(Routes.Detail.createRoute(id))` depuis `DashboardScreen`. Le retour utilise `navController.popBackStack()`.

Les options `popUpTo + saveState = true` et `launchSingleTop = true` sur les onglets de la `NavigationBar` évitent l'accumulation de destinations dans la back stack lors des changements d'onglet.

---

## 14. Mode hors-ligne et synchronisation

### Cycle de vie complet d'une opération hors-ligne

1. **Déclenchement** : l'utilisateur crée une fenêtre depuis `PlanifierFenetreDialog`. `PlanningViewModel.createFenetre()` appelle `repository.createFenetre(request)`.
2. **Tentative réseau** : `api.createFenetre(dto)` est appelé. Si l'API répond, la fenêtre est créée et retournée avec son ID définitif.
3. **Échec réseau** : une exception est levée. Le repository crée un placeholder local dans Room avec un ID négatif temporaire (`localId = -System.currentTimeMillis()`). L'opération est sérialisée et insérée dans `PendingOperationEntity`.
4. **Feedback utilisateur** : `repository.createFenetre()` retourne `Result.failure(OfflineException(...))`. Le ViewModel détecte `e is OfflineException` et positionne `_createSuccess = true` + `_pendingSync = true`. La Snackbar affiche "Fenêtre sauvegardée — sera envoyée à la reconnexion".
5. **Réapparition du réseau** : `NetworkConnectivityObserver` émet `true` via son `callbackFlow`. `AppContainer` collecte ce flow et appelle `repository.syncPendingOperations()`.
6. **Synchronisation FIFO** : `syncPendingOperations()` récupère toutes les opérations triées par `createdAt ASC` et les rejoue dans l'ordre. Pour `CREATE_FENETRE`, l'API est appelée, l'ID réel est reçu, le placeholder local est supprimé et la fenêtre réelle est insérée.
7. **Cohérence** : si une opération de synchronisation échoue (l'API est toujours indisponible), la boucle s'arrête et réessaiera au prochain retour réseau.

### NetworkConnectivityObserver

Utilise `callbackFlow` pour transformer les callbacks Android en Flow Kotlin. `ConnectivityManager.registerNetworkCallback()` est appelé dans le `callbackFlow` et `unregisterNetworkCallback()` est appelé dans `awaitClose {}` — ce qui garantit qu'il n'y a jamais de fuite de callback.

`distinctUntilChanged()` évite d'émettre deux fois le même état (connecté→connecté).

L'état initial est émis immédiatement via `trySend(isCurrentlyConnected())` pour que les abonnés aient toujours une valeur dès leur abonnement.

---

## 15. Validation des règles métier

L'application implémente trois règles métier de planification à trois niveaux différents :

### RG-F04 — Durée fenêtre [1–900 secondes]

**Niveau modèle** : `FenetreCom.DUREE_MIN_SECONDES = 1` et `DUREE_MAX_SECONDES = 900` sont des constantes du companion object.

**Niveau dialog** : `PlanifierFenetreDialog` valide `duree !in FenetreCom.DUREE_MIN_SECONDES..FenetreCom.DUREE_MAX_SECONDES` et affiche un `localError` dans la dialog.

**Niveau ViewModel** : `PlanningViewModel.createFenetre()` re-valide la durée avant l'appel API. Double validation pour une défense en profondeur.

### RG-S06 — Satellite non Désorbité

**Niveau modèle** : `Satellite.canScheduleWindow = statut != StatutSatellite.DESORBITE` — la règle est dans le modèle, pas dans l'UI.

**Niveau card** : `SatelliteCard` désactive les clics si `isDesorbite`. L'utilisateur ne peut même pas naviguer vers le détail d'un satellite Désorbité depuis la liste.

**Niveau dialog** : `PlanifierFenetreDialog` vérifie `!satellite.canScheduleWindow` avant de valider. Les satellites Désorbités sont affichés dans le dropdown avec une mention mais génèrent une erreur locale.

**Niveau ViewModel** : `PlanningViewModel.createFenetre()` re-valide via `satellite.canScheduleWindow`.

### RG-G03 — Station non en Maintenance/Inactive

**Niveau modèle** : `StationSol.canReceiveWindow = statut != MAINTENANCE && statut != INACTIVE`.

**Niveau dialog** : vérification de `!station.canReceiveWindow` avec message d'erreur explicite.

**Niveau ViewModel** : vérification de `station.statut == StatutStation.MAINTENANCE` avec message localisé incluant le nom de la station.

---

## 16. Fonctionnalités bonus

### Notifications WorkManager

`PassageNotificationWorker` est un `CoroutineWorker` planifié avec `PeriodicWorkRequestBuilder<PassageNotificationWorker>(5, TimeUnit.MINUTES)`.

La planification utilise `ExistingPeriodicWorkPolicy.KEEP` : si le worker est déjà planifié (par exemple après un redémarrage de l'app), il n'est pas dupliqué. Le worker est démarré depuis `MainActivity.onCreate()` via `PassageNotificationWorker.schedule(this)`.

Dans `doWork()`, le worker récupère le repository depuis `(context.applicationContext as NanoOrbitApplication).container.repository` et collecte les fenêtres Planifiées. Pour chaque fenêtre dont le début est dans les 15 prochaines minutes, une notification est envoyée.

Le canal de notification `passages_satellites` (IMPORTANCE_HIGH) est créé dans `NanoOrbitApplication.onCreate()` avec `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)`.

La permission `POST_NOTIFICATIONS` (Android 13+) est vérifiée avec `ActivityCompat.checkSelfPermission()` avant chaque envoi. Si elle n'est pas accordée, l'envoi est silencieusement ignoré.

L'ID de notification `satelliteNom.hashCode()` garantit que deux notifications pour le même satellite mettent à jour la même notification plutôt d'en créer une nouvelle.

### Pull-to-Refresh

`PullToRefreshBox` (composant Material3, `@ExperimentalMaterial3Api`) wrappant la `LazyColumn` sur `DashboardScreen` et `PlanningScreen`. Le paramètre `isRefreshing = isLoading` affiche/masque l'indicateur. `onRefresh = { viewModel.refreshSatellites() }` déclenche `forceRefresh()` sur le repository.

### Favoris DataStore Preferences

`FavoritesDataStore` utilise un `DataStore<Preferences>` créé via `preferencesDataStore(name = "favorites")`. Les IDs favoris sont stockés dans une `Set<String>` sous la clé `stringSetPreferencesKey("favorite_satellite_ids")`.

`favoriteIds` est un `Flow<Set<String>>` qui émet à chaque modification. Il est branché dans `DashboardViewModel` et converti en `StateFlow` pour être observé par la `SatelliteCard`.

`toggleFavorite(id)` utilise `dataStore.edit {}` qui garantit l'atomicité : si deux toggles arrivent simultanément, ils sont exécutés l'un après l'autre sans corruption des données.

---

## 17. Injection de dépendances (AppContainer)

L'application n'utilise pas de framework d'injection (Hilt, Dagger, Koin). La DI manuelle via `AppContainer` est suffisante pour ce projet et évite d'ajouter une dépendance supplémentaire.

`AppContainer` est instancié dans `NanoOrbitApplication.onCreate()` et stocké comme propriété `container` de l'Application. Les ViewModels y accèdent via `LocalContext.current.applicationContext as NanoOrbitApplication`.

```kotlin
class NanoOrbitApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        initOsmdroid()
        createNotificationChannels()
    }
}
```

`AppContainer` crée et câble toutes les dépendances dans l'ordre correct :

1. `NanoOrbitDatabase.getInstance(context)` — singleton Room
2. `RetrofitClient.create()` — instance Retrofit
3. `FavoritesDataStore(context)` — DataStore
4. `NetworkConnectivityObserver(context)` — observer réseau
5. `NanoOrbitRepositoryImpl(api, daos..., favoritesDataStore)` — repository

Le `containerScope` (`SupervisorJob + Dispatchers.IO`) collecte `networkObserver.isConnected` pour déclencher la synchronisation automatique au retour réseau. `SupervisorJob` garantit qu'une exception dans une coroutine enfant ne cancelle pas les autres.

---

## 18. Thème spatial

`NanoOrbitApplicationTheme` est un `darkColorScheme` Material3 avec une palette inspirée de l'espace :

| Rôle Material3 | Couleur | Hex |
|----------------|---------|-----|
| `primary` | StellarBlue | `#7DBAFF` |
| `secondary` | NebulaPurple | `#BBA9FF` |
| `tertiary` | AuroraTeal | `#80DDD8` |
| `background` | DeepSpace | `#080C1A` |
| `surface` | SpaceNavy | `#0D1228` |
| `surfaceContainer` | CosmosContainer | `#101830` |
| `error` | AlertRed | `#FF8A80` |
| `onBackground` | StarWhite | `#E2E8FF` |
| `onSurfaceVariant` | CosmicGray | `#8B96C0` |

Les couleurs de statut sémantiques, indépendantes du thème Material3, sont :

| Statut | Couleur | Hex |
|--------|---------|-----|
| `StatusGreen` | Opérationnel | `#69F0AE` |
| `StatusAmber` | En veille / Maintenance | `#FFB300` |
| `StatusRed` | Défaillant / Annulée | `#FF5252` |
| `StatusGrayBlue` | Désorbité / Inactif | `#546E7A` |
| `FenetreBlue` | Planifiée | `#448AFF` |
| `FenetreTeal` | Réalisée | `#00BFA5` |

`SideEffect { WindowCompat.getInsetsController(...).isAppearanceLightStatusBars = false }` force la barre de statut système en mode sombre pour correspondre au fond `#080C1A`.

---

## 19. Stack technique

| Composant | Bibliothèque | Version | Rôle |
|-----------|-------------|---------|------|
| Langage | Kotlin | 2.0 | — |
| UI | Jetpack Compose | BOM 2025.01 | Interfaces déclaratives |
| UI | Material3 | — | Composants, thème, couleurs |
| Architecture | ViewModel | 2.8 | Survie aux reconfigurations |
| Réactivité | Kotlin Coroutines | 1.9 | Asynchronisme structuré |
| Réactivité | StateFlow / Flow | — | Flux de données réactifs |
| Réseau | Retrofit 2 | 2.11 | Client HTTP typé |
| Réseau | OkHttp + Logging | 4.12 | Interceptors, timeouts, logs |
| Sérialisation | Gson | 2.10 | JSON ↔ Kotlin |
| Persistance | Room | 2.6 | Base de données SQLite type-safe |
| Persistance | DataStore Preferences | 1.1 | Clés-valeurs réactives (favoris) |
| Carte | osmdroid | 6.1 | OpenStreetMap sur Android |
| Background | WorkManager | 2.9 | Tâches périodiques garanties |
| Navigation | Navigation Compose | 2.8 | Pile de navigation typée |
| DI | Manuel (AppContainer) | — | Câblage des dépendances |
| API | nano-orbit.vlegall.fr | — | REST JSON, auth X-API-Key |
| minSdk | 26 (Android 8.0) | — | java.time natif, sans desugaring |
| targetSdk | 36 | — | Dernières APIs Android |

---

## 20. Progression depuis les TPs

Ce projet est l'aboutissement direct de quatre TPs réalisés sur un cas fil rouge Bordeaux VéloLib. Chaque TP a introduit un ensemble de concepts qui sont tous présents et approfondis dans NanoOrbit.

### TP1 — Fondations Android et Jetpack Compose

Ce premier TP introduit Kotlin, les fonctions `@Composable`, les `data class`, le cycle de vie Android et les listes avec `LazyColumn`. L'impact sur NanoOrbit est direct : tous les composants de l'application (`SatelliteCard`, `StatusBadge`, `FenetreCard`, `InstrumentItem`, `OfflineBanner`) sont des fonctions composables construites selon les principes introduits dans ce TP. L'utilisation de `LazyColumn` dans le Dashboard, le Planning et le DetailScreen résout le problème de performance posé dans ce TP : contrairement à `Column` qui compose tous les éléments en mémoire, `LazyColumn` ne compose que les éléments visibles à l'écran.

### TP2 — Données REST, MVVM et filtrage

Ce TP introduit Retrofit pour consommer une API REST (VéloLib de Bordeaux), le pattern ViewModel avec `StateFlow` et le filtrage en temps réel avec `combine()`. NanoOrbit pousse ces concepts beaucoup plus loin : 17 endpoints Retrofit au lieu de quelques-uns, `flatMapLatest()` pour les filtres dynamiques, `combine()` de quatre sources simultanées, et filtrage multi-critères (texte + statut + favoris). Le TP2 a aussi introduit le concept de barre de recherche en temps réel, directement présent dans `DashboardScreen`.

### TP2 Recherche — Optimisations et StateFlow avancé

Cette séance complémentaire approfondit les mécanismes de StateFlow, `SharingStarted` et les Factory de ViewModels. NanoOrbit applique systématiquement `SharingStarted.WhileSubscribed(5_000)` sur tous ses StateFlow pour la gestion précise du cycle de vie. Le pattern de Factory exposé ici est repris identiquement dans les quatre ViewModels de l'application.

### TP3 — Architecture MVVM complète, Room et offline

Ce TP est le pivot de la progression. Il introduit la séparation stricte en couches domain/data/presentation, le pattern Repository avec son interface, Room pour le cache local, et la gestion des états isLoading/errorMessage. NanoOrbit étend tous ces concepts : l'interface `NanoOrbitRepository` (principe SOLID-D), 7 entités Room au lieu de 2, la stratégie Network-First avec fallback, et la file FIFO d'opérations hors-ligne (`PendingOperationEntity`) qui va bien au-delà de ce qui était demandé dans le TP.

### TP4 — Carte OpenStreetMap avec osmdroid

Ce TP introduit l'intégration d'osmdroid dans Compose via `AndroidView`, les marqueurs personnalisés, la localisation GPS avec `MyLocationNewOverlay`, et la contrainte de scroll. NanoOrbit réutilise exactement ces mécanismes et les enrichit : marqueurs circulaires colorés générés dynamiquement via `android.graphics.Canvas` (pas d'icônes statiques), infobulles détaillées avec calcul de distance Haversine, et intégration des données temps réel de l'API REST pour les coordonnées et statuts des stations.

---

## 21. Évaluation

### Les 2 points perdus

**Point 1 — Absence de `delay(500)` artificiel** : le sujet demandait une simulation de latence pour démontrer le `CircularProgressIndicator`. Ce projet utilise une API REST réelle dont la latence réseau naturelle remplit ce rôle. En production, un délai artificiel serait une régression de l'expérience utilisateur.

**Point 2 — Network-First vs Cache-First** : le sujet spécifiait une stratégie Cache-First (données locales d'abord). Ce projet implémente Network-First (API d'abord, fallback Room) pour garantir la fraîcheur des données quand le réseau est disponible. Le comportement hors-ligne est strictement identique dans les deux stratégies : Room prend le relais sans interruption de service. Ce choix est justifié par l'existence d'une API réelle et opérationnelle.

### Points dépassant le cahier des charges

- 7 entités Room (minimum demandé : 2)
- 17 endpoints API REST (minimum demandé : quelques-uns)
- File FIFO hors-ligne avec synchronisation automatique au retour réseau
- Section "Historique des statuts" dans `DetailScreen` (non demandée)
- Marqueurs de carte colorés générés dynamiquement via `Canvas` (marqueurs statiques suffisaient)
- Calcul de distance Haversine dans les infobulles de la carte
- Contrainte de scroll osmdroid pour éviter la duplication du globe
- Favoris persistants avec `DataStore Preferences`
- Architecture multi-ViewModels (un ViewModel monolithique était suffisant)
- Thème spatial cohérent avec palette de 20 couleurs nommées