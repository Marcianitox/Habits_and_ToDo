@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.DeadlineConPlan
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// Leer desde muy atrás = incluir también las vencidas sin completar
private val DESDE_SIEMPRE: LocalDate = LocalDate.of(2000, 1, 1)

// Decide si se muestra la lista o el detalle de una deadline
@Composable
fun PantallaDeadlines() {
    var abierta by rememberSaveable { mutableStateOf<Long?>(null) }
    val idAbierta = abierta

    if (idAbierta != null) {
        PantallaDetalleDeadline(deadlineId = idAbierta, onVolver = { abierta = null })
    } else {
        ListaDeadlines(onAbrir = { abierta = it })
    }
}

@Composable
private fun ListaDeadlines(onAbrir: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = remember { BaseDeDatos.obtener(context).deadlineDao() }
    val hoy = remember { LocalDate.now() }
    val flujo = remember { dao.pendientesConPlan(DESDE_SIEMPRE) }
    val deadlines by flujo.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mostrandoNueva by remember { mutableStateOf(false) }

    val vencidas = deadlines.filter { it.deadline.fecha.isBefore(hoy) }
    val proximas = deadlines.filter { !it.deadline.fecha.isBefore(hoy) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (deadlines.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "No tenés deadlines pendientes",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Agregá un examen o una entrega con el botón de abajo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (vencidas.isNotEmpty()) {
                    item(key = "titulo-vencidas") {
                        EncabezadoLista(
                            titulo = "Vencidas",
                            detalle = "Marcalas como hechas desde su detalle",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(vencidas, key = { it.deadline.id }) { dcp ->
                        TarjetaDeadline(item = dcp, hoy = hoy, onAbrir = { onAbrir(dcp.deadline.id) })
                    }
                    item(key = "titulo-proximas") {
                        EncabezadoLista(
                            titulo = "Próximas",
                            detalle = null,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                if (proximas.isEmpty() && vencidas.isNotEmpty()) {
                    item(key = "sin-proximas") {
                        Text(
                            text = "No tenés deadlines próximas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                items(proximas, key = { it.deadline.id }) { dcp ->
                    TarjetaDeadline(item = dcp, hoy = hoy, onAbrir = { onAbrir(dcp.deadline.id) })
                }
            }
        }

        BotonAgregar(
            texto = "Nueva deadline",
            onClick = { mostrandoNueva = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (mostrandoNueva) {
        DialogoDeadline(
            inicial = null,
            onCancelar = { mostrandoNueva = false },
            onGuardar = { nueva ->
                scope.launch {
                    dao.insertar(nueva)
                    actualizarWidgets(context)
                }
                mostrandoNueva = false
            }
        )
    }
}

@Composable
private fun EncabezadoLista(
    titulo: String,
    detalle: String?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 4.dp)) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge, color = color)
        if (detalle != null) {
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaDeadline(item: DeadlineConPlan, hoy: LocalDate, onAbrir: () -> Unit) {
    val d = item.deadline
    val total = item.tareas.size
    val hechas = item.tareas.count { it.completada }
    val dias = ChronoUnit.DAYS.between(hoy, d.fecha)
    val vencida = dias < 0

    Card(
        onClick = onAbrir,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CuentaRegresiva(dias = dias)
            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = d.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etiqueta(
                        texto = nombreTipo(d.tipo),
                        fondo = MaterialTheme.colorScheme.secondaryContainer,
                        colorTexto = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    d.materia?.takeIf { it.isNotBlank() }?.let { materia ->
                        Text(
                            text = materia,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = if (vencida) "Venció ${textoDiasRestantes(d.fecha, hoy).lowercase()}"
                    else formatoFecha(d.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vencida) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (total == 0) {
                    Etiqueta(
                        texto = "Sin plan de acción",
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        colorTexto = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BarraProgreso(
                            fraccion = hechas.toFloat() / total,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$hechas/$total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Recuadro con los días. Girasol si faltan 3 o menos, coral si ya venció.
@Composable
private fun CuentaRegresiva(dias: Long) {
    val vencida = dias < 0
    val urgente = dias in 0..3
    val fondo = when {
        vencida -> MaterialTheme.colorScheme.errorContainer
        urgente -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val colorNumero = when {
        vencida -> MaterialTheme.colorScheme.onErrorContainer
        urgente -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fondo),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (dias == 0L) {
            Text(text = "Hoy", style = MaterialTheme.typography.titleMedium, color = colorNumero)
        } else {
            val cantidad = abs(dias)
            Text(text = "$cantidad", style = MaterialTheme.typography.headlineMedium, color = colorNumero)
            Text(
                text = if (cantidad == 1L) "día" else "días",
                style = MaterialTheme.typography.labelSmall,
                color = if (vencida) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Barra de progreso fina en menta (la usan también otras pantallas)
@Composable
internal fun BarraProgreso(fraccion: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraccion.coerceIn(0f, 1f) },
        modifier = modifier.height(6.dp),
        color = MaterialTheme.colorScheme.tertiary,
        trackColor = MaterialTheme.colorScheme.surface,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}