package fr.vlegall.nanoorbitapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import fr.vlegall.nanoorbitapplication.di.AppContainer
import org.osmdroid.config.Configuration

class NanoOrbitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        initOsmdroid()
        createNotificationChannels()
    }

    private fun initOsmdroid() {
        Configuration.getInstance().apply {
            load(this@NanoOrbitApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_PASSAGES_ID,
                "Passages satellites",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les fenêtres de communication imminentes"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_PASSAGES_ID = "passages_satellites"
    }
}