package fr.vlegall.nanoorbitapplication.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.vlegall.nanoorbitapplication.domain.model.FenetreCom
import fr.vlegall.nanoorbitapplication.domain.model.StatutFenetre
import fr.vlegall.nanoorbitapplication.mock.MockData
import fr.vlegall.nanoorbitapplication.ui.theme.CosmicGray
import fr.vlegall.nanoorbitapplication.ui.theme.FenetreBlue
import fr.vlegall.nanoorbitapplication.ui.theme.FenetreTeal
import fr.vlegall.nanoorbitapplication.ui.theme.NanoOrbitApplicationTheme
import fr.vlegall.nanoorbitapplication.ui.theme.StatusRed
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@Composable
fun FenetreCard(
    fenetre: FenetreCom,
    modifier: Modifier = Modifier
) {
    val statusColor = fenetre.statut.toStatusColor()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fenetre.datetimeDebut.format(DATE_FORMATTER),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                StatutFenetreBadge(statut = fenetre.statut)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabelValue(
                    label = "Satellite",
                    value = fenetre.nomSatellite ?: fenetre.idSatellite,
                    modifier = Modifier.weight(1f)
                )
                LabelValue(
                    label = "Station",
                    value = fenetre.nomStation ?: fenetre.codeStation,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                LabelValue(label = "Durée", value = fenetre.duree.formatDuree(), modifier = Modifier.weight(1f))
                LabelValue(label = "Élévation", value = "%.1f°".format(fenetre.elevationMax), modifier = Modifier.weight(1f))
                fenetre.volumeDonnees?.let { vol ->
                    LabelValue(label = "Volume", value = "%.0f Mo".format(vol), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            letterSpacing = 0.6.sp,
            color = CosmicGray,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatutFenetreBadge(statut: StatutFenetre, modifier: Modifier = Modifier) {
    val color = statut.toStatusColor()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = statut.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

fun StatutFenetre.toStatusColor(): Color = when (this) {
    StatutFenetre.PLANIFIEE -> FenetreBlue
    StatutFenetre.REALISEE  -> FenetreTeal
    StatutFenetre.ANNULEE   -> StatusRed
}

private fun Int.formatDuree(): String {
    val min = this / 60
    val sec = this % 60
    return if (min > 0) "${min}m ${sec}s" else "${sec}s"
}

@Preview(showBackground = true, backgroundColor = 0xFF080C1A)
@Composable
private fun FenetreCardPreview() {
    NanoOrbitApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MockData.fenetres.forEach { FenetreCard(fenetre = it) }
        }
    }
}