package com.juanti.organizador.recordatorios

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.juanti.organizador.MainActivity
import com.juanti.organizador.R
import com.juanti.organizador.data.BaseDeDatos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val CANAL = "deadlines"
private const val ETIQUETA = "recordatorio_deadline"
private const val CLAVE_ID = "id"
private const val CLAVE_DIAS = "dias"

// Leer desde muy atrás = todas las deadlines no completadas
private val DESDE_SIEMPRE: LocalDate = LocalDate.of(2000, 1, 1)

// Evita que dos reprogramaciones se pisen si se llaman casi al mismo tiempo
private val candado = Mutex()

// "Firma" de lo que determina los avisos programados la última vez.
// Si no cambió, no hace falta reprogramar nada.
@Volatile
private var ultimaFirma: String? = null

// Crea el canal de notificaciones (Android lo exige; si ya existe, no hace nada)
fun crearCanal(context: Context) {
    val canal = NotificationChannel(
        CANAL,
        "Recordatorios de deadlines",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Avisos antes de exámenes y entregas"
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
}

// Reprograma los avisos solo si cambió algo que los afecta
// (qué deadlines están pendientes, su fecha, sus recordatorios o su hora).
// Tildar hábitos o tareas no cambia la firma, así que no hace nada.
suspend fun reprogramarRecordatorios(context: Context) = candado.withLock {
    val deadlines = BaseDeDatos.obtener(context).deadlineDao()
        .pendientesConPlan(DESDE_SIEMPRE).first()
        .map { it.deadline }

    val firma = deadlines.joinToString("|") {
        "${it.id};${it.fecha};${it.recordatorios.sorted()};${it.horaRecordatorio}"
    }
    if (firma == ultimaFirma) return@withLock

    val trabajos = WorkManager.getInstance(context)
    trabajos.cancelAllWorkByTag(ETIQUETA)

    val ahora = LocalDateTime.now()
    for (d in deadlines) {
        for (dias in d.recordatorios) {
            val momento = d.fecha
                .minusDays(dias.toLong())
                .atTime(d.horaRecordatorio / 60, d.horaRecordatorio % 60)
            if (!momento.isAfter(ahora)) continue // ya pasó

            val espera = Duration.between(ahora, momento)
            val pedido = OneTimeWorkRequestBuilder<TrabajadorRecordatorio>()
                .setInitialDelay(espera.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(CLAVE_ID to d.id, CLAVE_DIAS to dias))
                .addTag(ETIQUETA)
                .build()

            trabajos.enqueueUniqueWork("recordatorio_${d.id}_$dias", ExistingWorkPolicy.REPLACE, pedido)
        }
    }

    ultimaFirma = firma
}

// Se ejecuta a la hora programada y muestra la notificación
// (el texto se arma en ese momento, con los datos actualizados)
class TrabajadorRecordatorio(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val id = inputData.getLong(CLAVE_ID, -1L)
        val dias = inputData.getInt(CLAVE_DIAS, 0)

        // Si la deadline ya no existe o ya está completada, no avisamos
        val dcp = BaseDeDatos.obtener(applicationContext).deadlineDao().conPlan(id).first()
            ?: return Result.success()
        val d = dcp.deadline
        if (d.completada) return Result.success()

        // Sin permiso de notificaciones no se puede mostrar nada
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        crearCanal(applicationContext)

        val cuando = when (dias) {
            0 -> "hoy"
            1 -> "mañana"
            7 -> "en una semana"
            else -> "en $dias días"
        }
        val total = dcp.tareas.size
        val hechas = dcp.tareas.count { it.completada }
        val texto = when {
            total == 0 -> "Todavía no tiene plan de acción."
            hechas == total -> "Completaste las $total sesiones del plan."
            else -> "Llevás $hechas de $total sesiones del plan."
        }

        // Tocar la notificación abre la app
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val alTocar = PendingIntent.getActivity(
            applicationContext,
            id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(R.drawable.ic_deadlines)
            .setColor(0xFFF2B632.toInt())
            .setContentTitle("${d.titulo}: $cuando")
            .setContentText(texto)
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify((id * 10 + dias).toInt(), notificacion)
        return Result.success()
    }
}