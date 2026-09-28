package com.juanti.organizador.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
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
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.Habito
import com.juanti.organizador.data.RegistroHabito
import com.juanti.organizador.data.TipoFrecuencia
import com.juanti.organizador.data.apareceHoy
import com.juanti.organizador.data.textoRacha
import com.juanti.organizador.data.vecesEnSemana
import com.juanti.organizador.ui.Seccion
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Desde cuándo leer registros (todo el historial, para las rachas)
private val REGISTROS_DESDE: LocalDate = LocalDate.of(2000, 1, 1)

// Widget "Hábitos": los hábitos de hoy, para tildar
class WidgetHabitos : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = BaseDeDatos.obtener(context).habitoDao()
        val hoy = LocalDate.now()

        val flujoHabitos = dao.activos()
        val flujoRegistros = dao.registrosEntre(REGISTROS_DESDE, hoy)
        val inicialHabitos = flujoHabitos.first()
        val inicialRegistros = flujoRegistros.first()

        provideContent {
            val habitos by flujoHabitos.collectAsState(initial = inicialHabitos)
            val registros by flujoRegistros.collectAsState(initial = inicialRegistros)

            val hechosPorHabito: Map<Long, Set<LocalDate>> =
                registros.groupBy({ it.habitoId }, { it.fecha }).mapValues { it.value.toSet() }

            // Los de hoy: primero los pendientes, después los hechos
            val deHoy = habitos
                .filter { it.apareceHoy(hoy, hechosPorHabito[it.id].orEmpty()) }
                .sortedBy { hoy in hechosPorHabito[it.id].orEmpty() }

            // Se lee en cada actualización, así un cambio de paleta se ve enseguida
            GlanceTheme(colors = coloresWidget(LocalContext.current)) {
                ContenidoHabitos(
                    hoy = hoy,
                    deHoy = deHoy,
                    hechosPorHabito = hechosPorHabito,
                    hayHabitos = habitos.isNotEmpty()
                )
            }
        }
    }
}

@Composable
private fun ContenidoHabitos(
    hoy: LocalDate,
    deHoy: List<Habito>,
    hechosPorHabito: Map<Long, Set<LocalDate>>,
    hayHabitos: Boolean
) {
    val hechosHoy = deHoy.count { hoy in hechosPorHabito[it.id].orEmpty() }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .padding(12.dp)
    ) {
        // Encabezado compacto (abre la sección Hábitos)
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(abrirSeccion(Seccion.HABITOS)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hábitos",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (deHoy.isNotEmpty()) {
                Text(
                    text = "$hechosHoy de ${deHoy.size} hoy",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (deHoy.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(abrirSeccion(Seccion.HABITOS)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hayHabitos) "Hoy no te toca ningún hábito 😌" else "Creá tus hábitos en la app",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(deHoy, itemId = { it.id }) { h ->
                    val hechos = hechosPorHabito[h.id].orEmpty()
                    FilaHabitoWidget(habito = h, hecho = hoy in hechos, hechos = hechos, hoy = hoy)
                }
            }
        }
    }
}

@Composable
private fun FilaHabitoWidget(habito: Habito, hecho: Boolean, hechos: Set<LocalDate>, hoy: LocalDate) {
    val semanal = habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA
    val detalle = if (semanal) {
        "${habito.vecesEnSemana(hoy, hechos)}/${habito.vecesPorSemana} esta semana"
    } else {
        habito.textoRacha(hoy, hechos)
    }

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
                        actionRunCallback<AccionAlternarHabito>(
                            actionParametersOf(CLAVE_ID to habito.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CasillaWidget(marcada = hecho, tamano = 30.dp)
            }
            Spacer(modifier = GlanceModifier.width(4.dp))
            // Nombre y racha (abren la sección Hábitos)
            Row(
                modifier = GlanceModifier.defaultWeight().clickable(abrirSeccion(Seccion.HABITOS)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habito.nombre,
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = if (hecho) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = detalle,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (semanal) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
        }
    }
}

// Se ejecuta al tocar la casilla de un hábito: lo marca o desmarca para hoy
class AccionAlternarHabito : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[CLAVE_ID] ?: return
        val dao = BaseDeDatos.obtener(context).habitoDao()
        val hoy = LocalDate.now()
        val registro = RegistroHabito(habitoId = id, fecha = hoy)

        val yaHecho = dao.registrosEntreLista(hoy, hoy).any { it.habitoId == id }
        if (yaHecho) dao.desregistrar(registro) else dao.registrar(registro)
        actualizarWidgets(context)
    }
}

class ReceptorHabitos : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetHabitos()
}