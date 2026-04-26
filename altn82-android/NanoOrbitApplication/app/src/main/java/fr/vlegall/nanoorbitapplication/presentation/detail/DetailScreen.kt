package fr.vlegall.nanoorbitapplication.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.Embarquement
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.HistoriqueStatut
import fr.vlegall.nanoorbitapplication.domain.model.Participation
import fr.vlegall.nanoorbitapplication.domain.model.Satellite
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.presentation.components.FenetreCard
import fr.vlegall.nanoorbitapplication.presentation.components.InstrumentItem
import fr.vlegall.nanoorbitapplication.presentation.components.StatusBadge
import fr.vlegall.nanoorbitapplication.ui.theme.CosmicGray
import fr.vlegall.nanoorbitapplication.ui.theme.StatusAmber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    satelliteId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.provideFactory(
            LocalContext.current.applicationContext as NanoOrbitApplication
        )
    )
) {
    LaunchedEffect(satelliteId) {
        viewModel.loadSatellite(satelliteId)
    }

    val satellite by viewModel.satellite.collectAsStateWithLifecycle()
    val instruments by viewModel.instruments.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val historique by viewModel.historique.collectAsStateWithLifecycle()
    val fenetres by viewModel.fenetres.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val anomalieSuccess by viewModel.anomalieSuccess.collectAsStateWithLifecycle()

    var showAnomalieDialog by remember { mutableStateOf(false) }
    var anomalieText by remember { mutableStateOf("") }

    if (anomalieSuccess) {
        LaunchedEffect(Unit) {
            showAnomalieDialog = false
            anomalieText = ""
            viewModel.dismissAnomalie()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        satellite?.nomSatellite ?: satelliteId,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading && satellite == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            errorMessage != null && satellite == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadSatellite(satelliteId) }) { Text("Réessayer") }
                }
            }

            satellite != null -> {
                DetailContent(
                    satellite = satellite!!,
                    instruments = instruments,
                    missions = missions,
                    historique = historique,
                    fenetres = fenetres,
                    onSignalerAnomalie = { showAnomalieDialog = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showAnomalieDialog) {
        AlertDialog(
            onDismissRequest = { showAnomalieDialog = false; anomalieText = "" },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Signaler une anomalie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Décrivez l'anomalie observée (5 à 1000 caractères)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = anomalieText,
                        onValueChange = { if (it.length <= 1000) anomalieText = it },
                        minLines = 3,
                        placeholder = { Text("Ex : perte de signal intermittente sur la bande X…") },
                        isError = anomalieText.isNotBlank() && anomalieText.length < 5,
                        supportingText = {
                            if (anomalieText.isNotBlank() && anomalieText.length < 5)
                                Text("Minimum 5 caractères")
                            else
                                Text("${anomalieText.length}/1000")
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        satellite?.let {
                            viewModel.signalerAnomalie(it.idSatellite, anomalieText)
                        }
                    },
                    enabled = anomalieText.length >= 5
                ) { Text("Signaler") }
            },
            dismissButton = {
                TextButton(onClick = { showAnomalieDialog = false; anomalieText = "" }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    satellite: Satellite,
    instruments: List<Embarquement>,
    missions: List<Participation>,
    historique: List<HistoriqueStatut>,
    fenetres: List<FenetreCom>,
    onSignalerAnomalie: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard(title = "Statut") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(statut = satellite.statut)
                    Column {
                        Text("Format : ${satellite.formatCubesat.label}", style = MaterialTheme.typography.bodyMedium)
                        satellite.orbite?.let { orb ->
                            Text(
                                "Orbite : ${orb.typeOrbite.label} — ${orb.altitude} km / ${orb.inclinaison}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Télémétrie") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    satellite.masse?.let { TelemetryRow("Masse", "${it} kg") }
                    satellite.dureeViePrevue?.let { duree ->
                        TelemetryRow("Durée de vie prévue", "$duree ans")
                        val anneesEcoulees = satellite.dateLancement?.let {
                            ChronoUnit.YEARS.between(it, LocalDate.now())
                        } ?: 0L
                        val restant = (duree - anneesEcoulees).coerceAtLeast(0)
                        TelemetryRow("Durée restante estimée", "$restant ans")
                    }
                    satellite.capaciteBatterie?.let { cap ->
                        TelemetryRow("Batterie", "${cap} Wh")
                        LinearProgressIndicator(
                            progress = { (cap / 100.0).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    satellite.dateLancement?.let { TelemetryRow("Lancement", it.toString()) }
                }
            }
        }

        if (instruments.isNotEmpty()) {
            item {
                Text(
                    "Instruments embarqués (${instruments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(instruments) { instrument ->
                InstrumentItem(instrument = instrument)
            }
        }

        if (missions.isNotEmpty()) {
            item {
                SectionCard(title = "Missions actives (${missions.size})") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        missions.forEach { p ->
                            Column {
                                Text(p.nomMission, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Rôle : ${p.roleSatellite}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                p.commentaire?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }

        if (historique.isNotEmpty()) {
            item {
                SectionCard(title = "Historique des statuts (${historique.size})") {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        historique.forEachIndexed { index, entry ->
                            HistoriqueStatutRow(entry)
                            if (index < historique.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        if (fenetres.isNotEmpty()) {
            item {
                Text(
                    "Fenêtres de communication récentes (${fenetres.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(fenetres.take(5)) { fenetre ->
                FenetreCard(fenetre = fenetre)
            }
        }

        item {
            OutlinedButton(
                onClick = onSignalerAnomalie,
                modifier = Modifier.fillMaxWidth(),
                enabled = satellite.statut != StatutSatellite.DESORBITE
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Signaler une anomalie")
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private val HISTORIQUE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
private fun HistoriqueStatutRow(entry: HistoriqueStatut) {
    val statutColor = when (entry.statut.uppercase()) {
        "OPERATIONNEL" -> MaterialTheme.colorScheme.primary
        "EN_VEILLE"    -> StatusAmber
        "DEFAILLANT"   -> MaterialTheme.colorScheme.error
        "DESORBITE"    -> CosmicGray
        else           -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = statutColor
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(
                entry.statut,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = statutColor
            )
        }
        Text(
            entry.timestamp.format(HISTORIQUE_FORMATTER),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}