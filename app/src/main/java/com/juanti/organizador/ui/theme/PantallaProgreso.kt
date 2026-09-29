@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.Habito
import com.juanti.organizador.data.Proporcion
import com.juanti.organizador.data.TipoFrecuencia
import com.juanti.organizador.data.evolucionSemanal
import com.juanti.organizador.data.mejorRacha
import com.juanti.organizador.data.proporcion
import com.juanti.organizador.data.proporcionTareas
import com.juanti.organizador.data.racha
import com.juanti.organizador.data.textoFrecuencia
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

// Desde cuándo leer registros de hábitos (todo el historial, para las rachas)
private val REGISTROS_DESDE: LocalDate = LocalDate.of(2000, 1, 1)

enum class Periodo(val etiqueta: String, val dias: Long) {
    SEMANA("7 días", 7),
    MES("30 días", 30),
    TRIMESTRE("90 días", 90)
}

// Las tres vistas de cada hábito (se pasa de una a otra deslizando)
private val VISTAS = listOf("Evolución", "Semana", "Mes")

@Composable
fun PantallaProgreso() {
    val context = LocalContext.current
    val bd = remember { BaseDeDatos.obtener(context) }
    val hoy = remember { LocalDate.now() }
    val desdeTareas = remember { hoy.minusDays(89) }

    val flujoTareas = remember { bd.tareaPlanDao().entre(desdeTareas, hoy) }
    val flujoHabitos = remember { bd.habitoDao().activos() }
    val flujoRegistros = remember { bd.habitoDao().registrosEntre(REGISTROS_DESDE, hoy) }
    val tareas by flujoTareas.collectAsState(initial = emptyList())
    val habitos by flujoHabitos.collectAsState(initial = emptyList())
    val registros by flujoRegistros.collectAsState(initial = emptyList())

    var periodo by rememberSaveable { mutableStateOf(Periodo.SEMANA) }
    val desde = hoy.minusDays(periodo.dias - 1)

    val hechosPorHabito: Map<Long, Set<LocalDate>> =
        registros.groupBy({ it.habitoId }, { it.fecha }).mapValues { it.value.toSet() }

    val propTareas = proporcionTareas(tareas, desde, hoy)
    val propHabitos = habitos.fold(Proporcion.VACIA) { acc, h ->
        acc + h.proporcion(hechosPorHabito[h.id].orEmpty(), desde, hoy)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Resumen general
        Grupo {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Cumplimiento", style = MaterialTheme.typography.titleLarge)
                SelectorSegmentado(
                    opciones = Periodo.entries.map { it.etiqueta },
                    seleccion = periodo.ordinal,
                    onSeleccion = { periodo = Periodo.entries[it] },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResumenAnillo(
                        titulo = "Plan de acción",
                        p = propTareas,
                        unidad = "tareas",
                        color = MaterialTheme.colorScheme.primary
                    )
                    ResumenAnillo(
                        titulo = "Hábitos",
                        p = propHabitos,
                        unidad = "veces",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Una tarjeta compacta por hábito
        if (habitos.isEmpty()) {
            Text(
                text = "Creá hábitos para ver su evolución acá.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        habitos.forEach { h ->
            key(h.id) {
                TarjetaProgresoHabito(
                    habito = h,
                    hechos = hechosPorHabito[h.id].orEmpty(),
                    hoy = hoy,
                    periodo = periodo,
                    desde = desde
                )
            }
        }
    }
}

@Composable
private fun TarjetaProgresoHabito(
    habito: Habito,
    hechos: Set<LocalDate>,
    hoy: LocalDate,
    periodo: Periodo,
    desde: LocalDate
) {
    val unidad = if (habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) "semanas" else "días"
    val racha = habito.racha(hoy, hechos)
    val mejor = habito.mejorRacha(hoy, hechos)
    val cumplimiento = habito.proporcion(hechos, desde, hoy)

    val estadoPaginas = rememberPagerState(pageCount = { VISTAS.size })
    val scope = rememberCoroutineScope()

    Grupo {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nombre y frecuencia
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = habito.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = habito.textoFrecuencia(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Datos en píldoras
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta(
                    texto = "🔥 $racha $unidad",
                    fondo = MaterialTheme.colorScheme.primaryContainer,
                    colorTexto = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Etiqueta(
                    texto = "🏆 Mejor: $mejor",
                    fondo = MaterialTheme.colorScheme.secondaryContainer,
                    colorTexto = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Etiqueta(
                    texto = "${textoPorcentaje(cumplimiento)} en ${periodo.etiqueta}",
                    fondo = MaterialTheme.colorScheme.tertiaryContainer,
                    colorTexto = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            // Selector de vista (también se puede deslizar)
            SelectorSegmentado(
                opciones = VISTAS,
                seleccion = estadoPaginas.currentPage,
                onSeleccion = { i -> scope.launch { estadoPaginas.animateScrollToPage(i) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Las tres vistas, una al lado de la otra
            HorizontalPager(
                state = estadoPaginas,
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) { pagina ->
                when (pagina) {
                    0 -> VistaEvolucion(habito = habito, hechos = hechos, hoy = hoy)
                    1 -> CalendarioHabito(habito = habito, hechos = hechos, hoy = hoy, vista = VistaCalendario.SEMANA)
                    else -> CalendarioHabito(habito = habito, hechos = hechos, hoy = hoy, vista = VistaCalendario.MES)
                }
            }
        }
    }
}

// Vista "Evolución": gráfico de las últimas 8 semanas + tendencia
@Composable
private fun VistaEvolucion(habito: Habito, hechos: Set<LocalDate>, hoy: LocalDate) {
    val evolucion = habito.evolucionSemanal(hechos, hoy, semanas = 8)
    val valores = evolucion.map { it.second }
    val etiquetas = evolucion.mapIndexed { i, (lunes, _) ->
        if (i == evolucion.lastIndex) "Actual" else "${lunes.dayOfMonth}/${lunes.monthValue}"
    }
    val (textoTendencia, diferencia) = tendenciaSemanal(valores)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GraficoLinea(valores = valores, etiquetas = etiquetas)
        Text(
            text = textoTendencia,
            style = MaterialTheme.typography.bodyMedium,
            color = colorTendencia(diferencia)
        )
    }
}

@Composable
private fun colorTendencia(diferencia: Int?): Color = when {
    diferencia == null -> MaterialTheme.colorScheme.onSurfaceVariant
    diferencia >= 5 -> MaterialTheme.colorScheme.tertiary
    diferencia <= -5 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ResumenAnillo(titulo: String, p: Proporcion, unidad: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Anillo(fraccion = p.fraccion, color = color)
        Text(text = titulo, style = MaterialTheme.typography.titleSmall)
        Text(
            text = if (p.esperados == 0) "Sin datos" else "${p.hechos} de ${p.esperados} $unidad",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun textoPorcentaje(p: Proporcion): String =
    p.fraccion?.let { "${(it * 100).roundToInt()}%" } ?: "—"

// Compara el promedio de las últimas 4 semanas con el de las 4 anteriores
private fun tendenciaSemanal(valores: List<Float?>): Pair<String, Int?> {
    val recientes = valores.takeLast(4).filterNotNull()
    val anteriores = valores.dropLast(4).takeLast(4).filterNotNull()
    if (recientes.isEmpty() || anteriores.isEmpty()) {
        return "Todavía no hay suficientes semanas para comparar." to null
    }
    val diferencia = ((recientes.average() - anteriores.average()) * 100).roundToInt()
    val texto = when {
        diferencia >= 5 -> "↑ Mejoraste $diferencia puntos respecto a las 4 semanas anteriores"
        diferencia <= -5 -> "↓ Bajaste ${-diferencia} puntos respecto a las 4 semanas anteriores"
        else -> "→ Te mantenés estable respecto a las 4 semanas anteriores"
    }
    return texto to diferencia
}