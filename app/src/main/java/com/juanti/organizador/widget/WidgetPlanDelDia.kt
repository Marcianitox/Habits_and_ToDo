package com.juanti.organizador.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale.forLanguageTag("es-AR")
private val FORMATO_DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE", ES)

// Cuántos días hacia adelante se puede navegar con las flechas
private const val MAX_DIAS_ADELANTE = 6

// Día que está mirando el widget: 0 = hoy, 1 = mañana, etc. (se guarda por widget)
private val CLAVE_DIAS_ADELANTE = intPreferencesKey("dias_adelante")

// Cuánto mover el día al tocar una flecha (-1 o +1)
private val CLAVE_PASO = ActionParameters.Key<Int>("paso")

// Widget "Plan del día": un día a la vez (con flechas) + tareas sin fecha al final
class WidgetPlanDelDia : GlanceAppWidget() {

    // Guarda en qué día quedó el widget
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bd = BaseDeDatos.obtener(context)
        val hoy = LocalDate.now()

        val flujoSemana = bd.tareaPlanDao().entre(hoy, hoy.plusDays(MAX_DIAS_ADELANTE.toLong()))
        val flujoAtrasadas = bd.tareaPlanDao().atrasadas(hoy)
        val flujoSinFecha = bd.tareaPlanDao().sinFecha()
        val flujoDeadlines = bd.deadlineDao().todas()
        val inicialSemana = flujoSemana.first()
        val inicialAtrasadas = flujoAtrasadas.first()
        val inicialSinFecha = flujoSinFecha.first()
        val inicialDeadlines = flujoDeadlines.first()

        provideContent {
            val semana by flujoSemana.collectAsState(initial = inicialSemana)
            val atrasadas by flujoAtrasadas.collectAsState(initial = inicialAtrasadas)
            val sinFecha by flujoSinFecha.collectAsState(initial = inicialSinFecha)
            val deadlines by flujoDeadlines.collectAsState(initial = inicialDeadlines)

            val diasAdelante = (currentState<Preferences>()[CLAVE_DIAS_ADELANTE] ?: 0)
                .coerceIn(0, MAX_DIAS_ADELANTE)
            val dia = hoy.plusDays(diasAdelante.toLong())
            val delDia = semana.filter { it.fecha == dia }

            // Se lee en cada actualización, así un cambio de paleta se ve enseguida
            GlanceTheme(colors = coloresWidget(LocalContext.current)) {
                ContenidoPlan(
                    hoy = hoy,
                    dia = dia,
                    diasAdelante = diasAdelante,
                    pendientes = delDia.filter { !it.completada },
                    hechasDelDia = delDia.count { it.completada },
                    atrasadas = if (dia == hoy) atrasadas else emptyList(),
                    sinFecha = sinFecha,
                    deadlinePorId = deadlines.associateBy { it.id }
                )
            }
        }
    }
}

@Composable
private fun ContenidoPlan(
    hoy: LocalDate,
    dia: LocalDate,
    diasAdelante: Int,
    pendientes: List<TareaPlan>,
    hechasDelDia: Int,
    atrasadas: List<TareaPlan>,
    sinFecha: List<TareaPlan>,
    deadlinePorId: Map<Long, Deadline>
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .padding(10.dp)
    ) {
        // Encabezado: ‹  día  ›  +
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonFlecha(
                icono = R.drawable.ic_anterior,
                descripcion = "Día anterior",
                habilitado = diasAdelante > 0,
                paso = -1
            )
            Column(
                modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.PLAN)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tituloDia(dia, hoy),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = formatoFecha(dia),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
            BotonFlecha(
                icono = R.drawable.ic_siguiente,
                descripcion = "Día siguiente",
                habilitado = diasAdelante < MAX_DIAS_ADELANTE,
                paso = 1
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .size(30.dp)
                    .cornerRadius(15.dp)
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
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            // Atrasadas (solo en "Hoy")
            items(atrasadas, itemId = { it.id }) { t ->
                FilaTareaWidget(
                    tarea = t,
                    deadline = t.deadlineId?.let { deadlinePorId[it] },
                    atrasada = true,
                    hoy = hoy
                )
            }

            // Tareas del día elegido
            items(pendientes, itemId = { it.id }) { t ->
                FilaTareaWidget(
                    tarea = t,
                    deadline = t.deadlineId?.let { deadlinePorId[it] },
                    atrasada = false,
                    hoy = hoy
                )
            }

            // Si el día no tiene nada
            if (pendientes.isEmpty() && atrasadas.isEmpty()) {
                item(itemId = Long.MAX_VALUE) {
                    Text(
                        text = when {
                            dia == hoy && hechasDelDia > 0 -> "¡Terminaste todo lo de hoy! 🎉"
                            dia == hoy -> "Nada planeado para hoy"
                            else -> "Nada planeado para este día"
                        },
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable(abrirSeccion(Seccion.PLAN)),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // Sin fecha (al final, scrolleando hacia abajo)
            if (sinFecha.isNotEmpty()) {
                item(itemId = Long.MAX_VALUE - 1) {
                    Text(
                        text = "Sin fecha",
                        modifier = GlanceModifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                items(sinFecha, itemId = { it.id }) { t ->
                    FilaTareaWidget(tarea = t, deadline = null, atrasada = false, hoy = hoy)
                }
            }
        }
    }
}

// Flecha del encabezado: botón circular con fondo suave y efecto al tocar.
// Cuando llega al límite, queda apagada y no reacciona.
@Composable
private fun BotonFlecha(icono: Int, descripcion: String, habilitado: Boolean, paso: Int) {
    // Capa de afuera: el círculo de fondo
    Box(
        modifier = GlanceModifier
            .size(32.dp)
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Capa de adentro: la zona táctil con el efecto circular gris
        var zonaTactil = GlanceModifier.fillMaxSize()
        if (habilitado) {
            zonaTactil = zonaTactil.clickable(
                onClick = actionRunCallback<AccionCambiarDia>(actionParametersOf(CLAVE_PASO to paso)),
                rippleOverride = R.drawable.widget_ripple_circulo
            )
        }
        Box(modifier = zonaTactil, contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(icono),
                contentDescription = descripcion,
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(
                    if (habilitado) GlanceTheme.colors.onSurface else GlanceTheme.colors.outline
                )
            )
        }
    }
}

// "Hoy", "Mañana" o el nombre del día ("Jueves")
private fun tituloDia(dia: LocalDate, hoy: LocalDate): String = when (dia) {
    hoy -> "Hoy"
    hoy.plusDays(1) -> "Mañana"
    else -> dia.format(FORMATO_DIA_SEMANA).replaceFirstChar { it.uppercase() }
}

@Composable
private fun FilaTareaWidget(tarea: TareaPlan, deadline: Deadline?, atrasada: Boolean, hoy: LocalDate) {
    val detalle = buildList {
        if (atrasada) add(textoDiasRestantes(tarea.fecha, hoy))
        if (deadline != null) add("⚑ ${deadline.titulo}")
    }.joinToString("   ")
    val tieneDescripcion = !tarea.descripcion.isNullOrBlank()

    // Column exterior = separación entre filas
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(14.dp)
                .padding(start = 2.dp, top = 2.dp, end = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Casilla (zona táctil de 38dp)
            Box(
                modifier = GlanceModifier
                    .size(38.dp)
                    .clickable(
                        actionRunCallback<AccionCompletarTarea>(
                            actionParametersOf(CLAVE_ID to tarea.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CasillaWidget(marcada = false, tamano = 26.dp)
            }
            Spacer(modifier = GlanceModifier.width(2.dp))
            // Texto (abre la sección Plan)
            Row(
                modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.PLAN)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = tarea.titulo,
                        maxLines = 2,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (detalle.isNotEmpty()) {
                        Text(
                            text = detalle,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (atrasada) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                // Marca de "tiene descripción"
                if (tieneDescripcion) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_descripcion),
                        contentDescription = "Tiene descripción",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                    )
                }
            }
        }
    }
}

// Se ejecuta al tocar una flecha: cambia el día que muestra este widget
class AccionCambiarDia : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val paso = parameters[CLAVE_PASO] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            val actual = prefs[CLAVE_DIAS_ADELANTE] ?: 0
            prefs[CLAVE_DIAS_ADELANTE] = (actual + paso).coerceIn(0, MAX_DIAS_ADELANTE)
        }
        WidgetPlanDelDia().update(context, glanceId)
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