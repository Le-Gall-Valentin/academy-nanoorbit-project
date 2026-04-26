package fr.vlegall.nanoorbitapplication.presentation.planning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.FenetreComCreateRequest
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import fr.vlegall.nanoorbitapplication.presentation.components.FenetreCard
import fr.vlegall.nanoorbitapplication.presentation.components.OfflineBanner
import fr.vlegall.nanoorbitapplication.ui.theme.CosmicGray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    viewModel: PlanningViewModel = viewModel(
        factory = PlanningViewModel.provideFactory(
            LocalContext.current.applicationContext as NanoOrbitApplication
        )
    )
) {
    val fenetres by viewModel.fenetres.collectAsStateWithLifecycle()
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val satellites by viewModel.satellites.collectAsStateWithLifecycle()
    val selectedStation by viewModel.selectedStation.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val createSuccess by viewModel.createSuccess.collectAsStateWithLifecycle()
    val pendingSync by viewModel.pendingSync.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(createSuccess) {
        if (createSuccess) {
            val message = if (pendingSync)
                "Fenêtre sauvegardée — sera envoyée à la reconnexion"
            else
                "Fenêtre planifiée avec succès"
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSuccess()
        }
    }

    val planifiees = fenetres.count { it.statut.name == "PLANIFIEE" }
    val realisees = fenetres.count { it.statut.name == "REALISEE" }
    val dureeTotale = fenetres.sumOf { it.duree }
    val volumeTotal = fenetres.sumOf { it.volumeDonnees ?: 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Planning communications",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFenetres() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Planifier") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OfflineBanner(isOffline = isOffline, lastSyncTimestamp = lastSyncTimestamp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatChip("Planifiées", planifiees.toString(), modifier = Modifier.weight(1f))
                StatChip("Réalisées", realisees.toString(), modifier = Modifier.weight(1f))
                StatChip("Durée", "${dureeTotale / 60}m ${dureeTotale % 60}s", modifier = Modifier.weight(1f))
                StatChip("Volume", "${"%.0f".format(volumeTotal)} Mo", modifier = Modifier.weight(1f))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStation == null,
                        onClick = { viewModel.onStationSelected(null) },
                        label = { Text("Toutes les stations") }
                    )
                }
                items(stations) { station ->
                    FilterChip(
                        selected = selectedStation == station.codeStation,
                        onClick = { viewModel.onStationSelected(station.codeStation) },
                        label = { Text(station.nomStation.split("—").first().trim()) }
                    )
                }
            }

            when {
                isLoading && fenetres.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.refreshFenetres() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (fenetres.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Aucune fenêtre de communication",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(fenetres, key = { it.idFenetre }) { fenetre ->
                                    FenetreCard(fenetre = fenetre)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlanifierFenetreDialog(
            satellites = satellites,
            stations = stations,
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                viewModel.createFenetre(request)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = CosmicGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val DATETIME_INPUT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
private fun PlanifierFenetreDialog(
    satellites: List<SatelliteSummary>,
    stations: List<StationSol>,
    onDismiss: () -> Unit,
    onConfirm: (FenetreComCreateRequest) -> Unit
) {
    val selectableSatellites = satellites.filter { it.canScheduleWindow }
    val selectableStations = stations.filter { it.canReceiveWindow }

    var satelliteMenuExpanded by remember { mutableStateOf(false) }
    var stationMenuExpanded by remember { mutableStateOf(false) }

    var selectedSatelliteId by remember(satellites) {
        mutableStateOf(
            selectableSatellites.firstOrNull()?.idSatellite
                ?: satellites.firstOrNull()?.idSatellite.orEmpty()
        )
    }
    var selectedStationCode by remember(stations) {
        mutableStateOf(
            selectableStations.firstOrNull()?.codeStation
                ?: stations.firstOrNull()?.codeStation.orEmpty()
        )
    }

    var datetimeInput by remember {
        mutableStateOf(LocalDateTime.now().plusMinutes(15).format(DATETIME_INPUT_FORMAT))
    }
    var dureeInput by remember { mutableStateOf("600") }
    var elevationInput by remember { mutableStateOf("45.0") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planifier une fenêtre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { satelliteMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = satellites.firstOrNull { it.idSatellite == selectedSatelliteId }?.let {
                        "${it.nomSatellite} (${it.idSatellite})"
                    } ?: "Choisir un satellite"
                    Text(label)
                }
                DropdownMenu(
                    expanded = satelliteMenuExpanded,
                    onDismissRequest = { satelliteMenuExpanded = false }
                ) {
                    satellites.forEach { sat ->
                        DropdownMenuItem(
                            text = { Text("${sat.nomSatellite} (${sat.idSatellite})") },
                            onClick = {
                                selectedSatelliteId = sat.idSatellite
                                satelliteMenuExpanded = false
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { stationMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = stations.firstOrNull { it.codeStation == selectedStationCode }?.let {
                        "${it.nomStation} (${it.codeStation})"
                    } ?: "Choisir une station"
                    Text(label)
                }
                DropdownMenu(
                    expanded = stationMenuExpanded,
                    onDismissRequest = { stationMenuExpanded = false }
                ) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text("${station.nomStation} (${station.codeStation})") },
                            onClick = {
                                selectedStationCode = station.codeStation
                                stationMenuExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = datetimeInput,
                    onValueChange = { datetimeInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Début (yyyy-MM-dd HH:mm)") }
                )
                OutlinedTextField(
                    value = dureeInput,
                    onValueChange = { dureeInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Durée (1-900 s)") }
                )
                OutlinedTextField(
                    value = elevationInput,
                    onValueChange = { elevationInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Élévation max (°)") }
                )
                localError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val satellite = satellites.firstOrNull { it.idSatellite == selectedSatelliteId }
                    if (satellite == null) { localError = "Sélectionne un satellite."; return@Button }
                    if (!satellite.canScheduleWindow) { localError = "Satellite désorbité: planification interdite (RG-S06)."; return@Button }

                    val station = stations.firstOrNull { it.codeStation == selectedStationCode }
                    if (station == null) { localError = "Sélectionne une station."; return@Button }
                    if (!station.canReceiveWindow) { localError = "Station indisponible: maintenance/inactive (RG-G03)."; return@Button }

                    val datetime = try {
                        LocalDateTime.parse(datetimeInput, DATETIME_INPUT_FORMAT)
                    } catch (_: DateTimeParseException) { null }
                    if (datetime == null) { localError = "Date invalide. Format attendu: yyyy-MM-dd HH:mm."; return@Button }

                    val duree = dureeInput.toIntOrNull()
                    if (duree == null || duree !in FenetreCom.DUREE_MIN_SECONDES..FenetreCom.DUREE_MAX_SECONDES) {
                        localError = "Durée invalide: valeur attendue entre 1 et 900 secondes (RG-F04)."; return@Button
                    }

                    val elevation = elevationInput.toDoubleOrNull()
                    if (elevation == null || elevation !in 0.0..90.0) {
                        localError = "Élévation invalide: valeur attendue entre 0 et 90°."; return@Button
                    }

                    localError = null
                    onConfirm(
                        FenetreComCreateRequest(
                            idSatellite = satellite.idSatellite,
                            codeStation = station.codeStation,
                            datetimeDebut = datetime,
                            duree = duree,
                            elevationMax = elevation
                        )
                    )
                },
                enabled = satellites.isNotEmpty() && stations.isNotEmpty()
            ) {
                Text("Planifier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}