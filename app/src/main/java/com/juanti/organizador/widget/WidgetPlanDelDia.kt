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
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import com.juanti.organizador.data.Deadline
import com.juanti.organizador.data.TareaPlan
import com.juanti.organizador.ui.Seccion
import com.juanti.organizador.ui.formatoFecha
import com.juanti.organizador.ui.textoDiasRestantes
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Widget "Plan del día": tareas de hoy + atrasadas, con botón para agregar
class WidgetPlanDelDia : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bd = BaseDeDatos.obtener(context)
        val hoy = LocalDate.now()

        val flujoHoy = bd.tareaPlanDao().delDia(hoy)
        val flujoAtrasadas = bd.tareaPlanDao().atrasadas(hoy)
        val flujoDeadlines = bd.deadlineDao().todas()
        val inicialHoy = flujoHoy.first()
        val inicialAtrasadas = flujoAtrasadas.first()
        val inicialDeadlines = flujoDeadlines.first()

        provideContent {
            val deHoy by flujoHoy.collectAsState(initial = inicialHoy)
            val atrasadas by flujoAtrasadas.collectAsState(initial = inicialAtrasadas)
            val deadlines by flujoDeadlines.collectAsState(initial = inicialDeadlines)

            // Se lee en cada actualización, así un cambio de paleta se ve enseguida
            GlanceTheme(colors = coloresWidget(LocalContext.current)) {
                ContenidoPlan(
                    hoy = hoy,
                    pendientes = deHoy.filter { !it.completada },
                    atrasadas = atrasadas,
                    deadlinePorId = deadlines.associateBy { it.id },
                    hechasHoy = deHoy.count { it.completada }
                )
            }
        }
    }
}

@Composable
private fun ContenidoPlan(
    hoy: LocalDate,
    pendientes: List<TareaPlan>,
    atrasadas: List<TareaPlan>,
    deadlinePorId: Map<Long, Deadline>,
    hechasHoy: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        // Encabezado compacto: "Hoy" + fecha (abre Plan) y botón +
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.PLAN))) {
                Text(
                    text = "Hoy",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = formatoFecha(hoy),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(34.dp)
                    .cornerRadius(17.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(
                        actionStartActivity<ActividadCargaRapida>(
                            actionParametersOf(CLAVE_TIPO to TIPO_TAREA)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_agregar),
                    contentDescription = "Agregar tarea",
                    modifier = GlanceModifier.size(20.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (pendientes.isEmpty() && atrasadas.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(abrirSeccion(Seccion.PLAN)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hechasHoy > 0) "¡Terminaste todo lo de hoy! 🎉" else "Nada planeado para hoy",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(atrasadas, itemId = { it.id }) { t ->
                    FilaTareaWidget(
                        tarea = t,
                        deadline = t.deadlineId?.let { deadlinePorId[it] },
                        atrasada = true,
                        hoy = hoy
                    )
                }
                items(pendientes, itemId = { it.id }) { t ->
                    FilaTareaWidget(
                        tarea = t,
                        deadline = t.deadlineId?.let { deadlinePorId[it] },
                        atrasada = false,
                        hoy = hoy
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaTareaWidget(tarea: TareaPlan, deadline: Deadline?, atrasada: Boolean, hoy: LocalDate) {
    val detalle = buildList {
        if (atrasada) add(textoDiasRestantes(tarea.fecha, hoy))
        if (deadline != null) add("⚑ ${deadline.titulo}")
    }.joinToString("   ")

    // Column exterior = separación entre filas
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(16.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Casilla (zona táctil de 44dp)
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .clickable(
                        actionRunCallback<AccionCompletarTarea>(
                            actionParametersOf(CLAVE_ID to tarea.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CasillaWidget(marcada = false, tamano = 30.dp)
            }
            Spacer(modifier = GlanceModifier.width(4.dp))
            // Texto (abre la sección Plan)
            Column(modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.PLAN))) {
                Text(
                    text = tarea.titulo,
                    maxLines = 2,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (detalle.isNotEmpty()) {
                    Text(
                        text = detalle,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (atrasada) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// Se ejecuta al tocar la casilla de una tarea en el widget
class AccionCompletarTarea : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[CLAVE_ID] ?: return
        BaseDeDatos.obtener(context).tareaPlanDao().marcar(id, true)
        actualizarWidgets(context)
    }
}

class ReceptorPlanDelDia : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetPlanDelDia()
}