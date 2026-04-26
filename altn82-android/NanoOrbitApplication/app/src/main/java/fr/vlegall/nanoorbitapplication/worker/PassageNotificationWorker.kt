package fr.vlegall.nanoorbitapplication.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.vlegall.nanoorbitapplication.MainActivity
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.StatutFenetre
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class PassageNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (context.applicationContext as NanoOrbitApplication).container.repository

        repository.getFenetresStream(statut = StatutFenetre.PLANIFIEE.label).collect { fenetres ->
            val now = LocalDateTime.now()
            fenetres.filter { fenetre ->
                val minutesUntilStart = ChronoUnit.MINUTES.between(now, fenetre.datetimeDebut)
                minutesUntilStart in 0..15L
            }.forEach { fenetre ->
                sendPassageNotification(
                    satelliteNom = fenetre.nomSatellite ?: fenetre.idSatellite,
                    stationNom = fenetre.nomStation ?: fenetre.codeStation,
                    duree = fenetre.duree,
                    minutesRestantes = ChronoUnit.MINUTES.between(now, fenetre.datetimeDebut).toInt()
                )
            }
            return@collect
        }

        return Result.success()
    }

    private fun sendPassageNotification(
        satelliteNom: String,
        stationNom: String,
        duree: Int,
        minutesRestantes: Int
    ) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NanoOrbitApplication.CHANNEL_PASSAGES_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Passage imminent — $satelliteNom")
            .setContentText("Via $stationNom · Dans $minutesRestantes min · Durée ${duree}s")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(satelliteNom.hashCode(), notification)
    }

    companion object {
        private const val WORK_NAME = "passage_notifications"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PassageNotificationWorker>(
                5, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}