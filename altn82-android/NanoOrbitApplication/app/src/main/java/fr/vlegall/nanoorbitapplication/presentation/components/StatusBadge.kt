package fr.vlegall.nanoorbitapplication.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.vlegall.nanoorbitapplication.domain.model.StatutSatellite
import fr.vlegall.nanoorbitapplication.ui.theme.NanoOrbitApplicationTheme
import fr.vlegall.nanoorbitapplication.ui.theme.StatusAmber
import fr.vlegall.nanoorbitapplication.ui.theme.StatusGrayBlue
import fr.vlegall.nanoorbitapplication.ui.theme.StatusGreen
import fr.vlegall.nanoorbitapplication.ui.theme.StatusRed

@Composable
fun StatusBadge(
    statut: StatutSatellite,
    modifier: Modifier = Modifier
) {
    val color = statut.toColor()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = statut.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

fun StatutSatellite.toColor(): Color = when (this) {
    StatutSatellite.OPERATIONNEL -> StatusGreen
    StatutSatellite.EN_VEILLE    -> StatusAmber
    StatutSatellite.DEFAILLANT   -> StatusRed
    StatutSatellite.DESORBITE    -> StatusGrayBlue
}

@Preview(showBackground = true, backgroundColor = 0xFF080C1A)
@Composable
private fun StatusBadgePreview() {
    NanoOrbitApplicationTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatutSatellite.entries.forEach { statut ->
                StatusBadge(statut = statut)
            }
        }
    }
}