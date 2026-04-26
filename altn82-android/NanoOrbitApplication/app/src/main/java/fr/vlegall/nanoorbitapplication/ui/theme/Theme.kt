package fr.vlegall.nanoorbitapplication.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpaceColorScheme = darkColorScheme(
    primary                = StellarBlue,
    onPrimary              = OnStellarBlue,
    primaryContainer       = StellarBlueContainer,
    onPrimaryContainer     = OnStellarBlueContainer,
    secondary              = NebulaPurple,
    onSecondary            = OnNebulaPurple,
    secondaryContainer     = NebulaContainer,
    onSecondaryContainer   = OnNebulaContainer,
    tertiary               = AuroraTeal,
    onTertiary             = OnAuroraTeal,
    tertiaryContainer      = AuroraContainer,
    onTertiaryContainer    = OnAuroraContainer,
    error                  = AlertRed,
    onError                = OnAlertRed,
    errorContainer         = AlertRedContainer,
    onErrorContainer       = OnAlertRedContainer,
    background             = DeepSpace,
    onBackground           = StarWhite,
    surface                = SpaceNavy,
    onSurface              = StarWhite,
    surfaceVariant         = CosmosDark,
    onSurfaceVariant       = CosmicGray,
    surfaceContainer       = CosmosContainer,
    surfaceContainerHigh   = CosmosContainerH,
    surfaceContainerHighest= CosmosContainerX,
    outline                = SpaceOutline,
    outlineVariant         = SpaceOutlineVariant,
    scrim                  = Color.Black,
    inverseSurface         = StarWhite,
    inverseOnSurface       = DeepSpace,
    inversePrimary         = StellarBlueContainer,
)

@Composable
fun NanoOrbitApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SpaceColorScheme,
        typography = Typography,
        content = content
    )
}