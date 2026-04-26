package fr.vlegall.nanoorbitapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.vlegall.nanoorbitapplication.navigation.NanoOrbitNavHost
import fr.vlegall.nanoorbitapplication.ui.theme.NanoOrbitApplicationTheme
import fr.vlegall.nanoorbitapplication.worker.PassageNotificationWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PassageNotificationWorker.schedule(this)
        setContent {
            NanoOrbitApplicationTheme {
                NanoOrbitNavHost()
            }
        }
    }
}