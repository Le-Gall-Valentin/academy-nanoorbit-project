package fr.vlegall.nanoorbitapplication.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.vlegall.nanoorbitapplication.ui.theme.NanoOrbitApplicationTheme
import fr.vlegall.nanoorbitapplication.ui.theme.StatusAmber
import java.util.concurrent.TimeUnit

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    lastSyncTimestamp: Long?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isOffline) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(StatusAmber.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildString {
                        append("Mode hors-ligne")
                        lastSyncTimestamp?.let { ts ->
                            val ageMin = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - ts)
                            append(" · Mis à jour il y a ${ageMin}m")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080C1A)
@Composable
private fun OfflineBannerPreview() {
    NanoOrbitApplicationTheme {
        OfflineBanner(
            isOffline = true,
            lastSyncTimestamp = System.currentTimeMillis() - 7 * 60 * 1000L
        )
    }
}