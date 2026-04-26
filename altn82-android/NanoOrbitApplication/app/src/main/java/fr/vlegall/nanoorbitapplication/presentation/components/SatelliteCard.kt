package fr.vlegall.nanoorbitapplication.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.vlegall.nanoorbitapplication.domain.model.SatelliteSummary
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.mock.MockData
import fr.vlegall.nanoorbitapplication.ui.theme.CosmicGray
import fr.vlegall.nanoorbitapplication.ui.theme.NanoOrbitApplicationTheme

@Composable
fun SatelliteCard(
    satellite: SatelliteSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {}
) {
    val isDesorbite = satellite.statut == StatutSatellite.DESORBITE
    val statusColor = satellite.statut.toColor()
    val cardAlpha = if (isDesorbite) 0.5f else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(enabled = !isDesorbite, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = if (isDesorbite) 0.2f else 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Satellite,
                contentDescription = null,
                tint = statusColor.copy(alpha = if (isDesorbite) 0.4f else 0.9f),
                modifier = Modifier.size(26.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = satellite.nomSatellite,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDesorbite) CosmicGray
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDesorbite) {
                        Text(
                            text = "DÉSORBITÉ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = CosmicGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = satellite.idSatellite,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall, color = CosmicGray)
                    Text(
                        text = satellite.formatCubesat.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicGray,
                        maxLines = 1
                    )
                }
                satellite.orbite?.let { orbite ->
                    Text(
                        text = "${orbite.typeOrbite.label} · ${orbite.altitude} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                    tint = if (isFavorite) Color(0xFFFFD600)
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            StatusBadge(statut = satellite.statut)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080C1A)
@Composable
private fun SatelliteCardPreview() {
    NanoOrbitApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MockData.satellites.take(3).forEach { sat ->
                SatelliteCard(satellite = sat, onClick = {})
            }
            SatelliteCard(
                satellite = MockData.satellites.last { it.statut == StatutSatellite.DESORBITE },
                onClick = {}
            )
        }
    }
}