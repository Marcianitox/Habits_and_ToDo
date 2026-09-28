package com.juanti.organizador.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.juanti.organizador.ActividadCargaRapida
import com.juanti.organizador.R
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.DeadlineConPlan
import com.juanti.organizador.ui.Seccion
import com.juanti.organizador.ui.nombreTipo
import com.juanti.organizador.ui.textoDiasRestantes
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Qué hace la ventanita cuando se toca la casilla de una deadline
const val TIPO_COMPLETAR_DEADLINE = "completar_deadline"

// Leer desde muy atrás = incluir también las vencidas sin completar
private val DESDE_SIEMPRE: LocalDate = LocalDate.of(2000, 1, 1)

// Widget "Deadlines": pendientes (incluidas vencidas), agregar y completar
class WidgetDeadlines : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = BaseDeDatos.obtener(context).deadlineDao()
        val hoy = LocalDate.now()
        val flujo = dao.pendientesConPlan(DESDE_SIEMPRE)
        val inicial = flujo.first()

        provideContent {
            val deadlines by flujo.collectAsState(initial = inicial)
            GlanceTheme(colors = ColoresWidget) {
                ContenidoDeadlines(hoy = hoy, deadlines = deadlines)
            }
        }
    }
}

@Composable
private fun ContenidoDeadlines(hoy: LocalDate, deadlines: List<DeadlineConPlan>) {
    val vencidas = deadlines.count { it.deadline.fecha.isBefore(hoy) }
    val subtitulo = when {
        deadlines.isEmpty() -> "Sin pendientes"
        vencidas > 0 -> "${deadlines.size} pendientes · $vencidas vencidas"
        else -> "${deadlines.size} pendientes"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .padding(14.dp)
    ) {
        // Encabezado (abre la sección Deadlines) + botón +
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.DEADLINES))) {
                Text(
                    text = "Deadlines",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitulo,
                    style = TextStyle(
                        color = if (vencidas > 0) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(20.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(
                        actionStartActivity<ActividadCargaRapida>(
                            actionParametersOf(CLAVE_TIPO to TIPO_DEADLINE)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_agregar),
                    contentDescription = "Agregar deadline",
                    modifier = GlanceModifier.size(22.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (deadlines.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(abrirSeccion(Seccion.DEADLINES)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tenés deadlines pendientes",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(deadlines, itemId = { it.deadline.id }) { dcp ->
                    FilaDeadlineWidget(item = dcp, hoy = hoy)
                }
            }
        }
    }
}

@Composable
private fun FilaDeadlineWidget(item: DeadlineConPlan, hoy: LocalDate) {
    val d = item.deadline
    val dias = ChronoUnit.DAYS.between(hoy, d.fecha)
    val vencida = dias < 0
    val urgente = dias in 0..3
    val total = item.tareas.size
    val hechas = item.tareas.count { it.completada }

    val fondoCuenta = when {
        vencida -> GlanceTheme.colors.errorContainer
        urgente -> GlanceTheme.colors.primaryContainer
        else -> GlanceTheme.colors.background
    }
    val lineaTipo = if (vencida) {
        "Venció ${textoDiasRestantes(d.fecha, hoy).lowercase()}"
    } else {
        listOfNotNull(nombreTipo(d.tipo), d.materia?.takeIf { it.isNotBlank() }).joinToString(" · ")
    }

    // Column exterior = separación entre filas
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(14.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cuenta regresiva
            Column(
                modifier = GlanceModifier
                    .size(46.dp)
                    .cornerRadius(12.dp)
                    .background(fondoCuenta),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    vencida -> Text(
                        text = "!",
                        style = TextStyle(
                            color = GlanceTheme.colors.onErrorContainer,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    dias == 0L -> Text(
                        text = "Hoy",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    else -> {
                        Text(
                            text = "$dias",
                            style = TextStyle(
                                color = if (urgente) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (dias == 1L) "día" else "días",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Datos (abren la sección Deadlines)
            Column(modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.DEADLINES))) {
                Text(
                    text = d.titulo,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = lineaTipo,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (vencida) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = if (total == 0) "Sin plan de acción" else "$hechas/$total sesiones",
                    maxLines = 1,
                    style = TextStyle(
                        color = if (total == 0) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.tertiary,
                        fontSize = 11.sp
                    )
                )
            }

            // Completar (abre una confirmación)
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .clickable(
                        actionStartActivity<ActividadCargaRapida>(
                            actionParametersOf(
                                CLAVE_TIPO to TIPO_COMPLETAR_DEADLINE,
                                CLAVE_ID to d.id
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_casilla_vacia),
                    contentDescription = "Marcar como completada",
                    modifier = GlanceModifier.size(24.dp)
                )
            }
        }
    }
}

class ReceptorDeadlines : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetDeadlines()
}