package fr.vlegall.nanoorbitapplication.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.vlegall.nanoorbitapplication.domain.model.Embarquement
import fr.vlegall.nanoorbitapplication.mock.MockData
import fr.vlegall.nanoorbitapplication.ui.theme.CosmicGray
import fr.vlegall.nanoorbitapplication.ui.theme.NanoOrbitApplicationTheme
import fr.vlegall.nanoorbitapplication.ui.theme.StatusAmber
import fr.vlegall.nanoorbitapplication.ui.theme.StatusGreen
import fr.vlegall.nanoorbitapplication.ui.theme.StatusRed

@Composable
fun InstrumentItem(
    instrument: Embarquement,
    modifier: Modifier = Modifier
) {
    val etatColor = instrument.etatFonctionnement.toEtatColor()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, etatColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = etatColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = instrument.typeInstrument,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    EtatBadge(etat = instrument.etatFonctionnement)
                }

                Text(
                    text = instrument.modele,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicGray
                )

                instrument.commentaire?.let { comment ->
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicGray
                    )
                }
            }
        }
    }
}

@Composable
private fun EtatBadge(etat: String) {
    Text(
        text = etat,
        style = MaterialTheme.typography.labelSmall,
        color = etat.toEtatColor(),
        fontWeight = FontWeight.SemiBold
    )
}

fun String.toEtatColor(): Color = when (this.lowercase()) {
    "nominal"                  -> StatusGreen
    "dégradé", "degrade"      -> StatusAmber
    "hors service", "en panne" -> StatusRed
    else                       -> CosmicGray
}

@Preview(showBackground = true, backgroundColor = 0xFF080C1A)
@Composable
private fun InstrumentItemPreview() {
    NanoOrbitApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MockData.instruments.forEach { InstrumentItem(instrument = it) }
        }
    }
}