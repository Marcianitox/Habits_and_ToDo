@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.juanti.organizador.R
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.Deadline
import com.juanti.organizador.data.TareaPlan
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DIAS_VISIBLES = 14L
private val ES = Locale.forLanguageTag("es-AR")
private val FORMATO_DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE", ES)
private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", ES)

@Composable
fun PantallaPlan() {
    val context = LocalContext.current
    val bd = remember { BaseDeDatos.obtener(context) }
    val hoy = remember { LocalDate.now() }
    val hasta = remember { hoy.plusDays(DIAS_VISIBLES - 1) }

    val flujoTareas = remember { bd.tareaPlanDao().entre(hoy, hasta) }
    val flujoAtrasadas = remember { bd.tareaPlanDao().atrasadas(hoy) }
    val flujoSinFecha = remember { bd.tareaPlanDao().sinFecha() }
    val flujoDeadlines = remember { bd.deadlineDao().todas() }
    val tareas by flujoTareas.collectAsState(initial = emptyList())
    val atrasadas by flujoAtrasadas.collectAsState(initial = emptyList())
    val sinFecha by flujoSinFecha.collectAsState(initial = emptyList())
    val deadlines by flujoDeadlines.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val avisos = remember { SnackbarHostState() }
    var mostrandoNueva by remember { mutableStateOf(false) }
    var tareaEditando by remember { mutableStateOf<TareaPlan?>(null) }

    // Tareas con la descripción desplegada
    var expandidas by remember { mutableStateOf(setOf<Long>()) }
    fun alternarDescripcion(t: TareaPlan) {
        expandidas = if (t.id in expandidas) expandidas - t.id else expandidas + t.id
    }

    val deadlinePorId = deadlines.associateBy { it.id }
    // Deadlines a las que se puede vincular una tarea: pendientes y no vencidas
    val deadlinesVinculables = deadlines.filter { !it.completada && !it.fecha.isBefore(hoy) }

    // Solo se muestran las pendientes; las hechas quedan guardadas para las estadísticas
    val pendientes = tareas.filter { !it.completada }
    val hechasHoy = tareas.count { it.fecha == hoy && it.completada }
    val porDia = pendientes.groupBy { it.fecha }
    val dias = (porDia.keys + hoy).sorted() // "Hoy" aparece siempre

    fun completar(t: TareaPlan) = scope.launch {
        bd.tareaPlanDao().marcar(t.id, true)
        actualizarWidgets(context)
        avisos.currentSnackbarData?.dismiss()
        val resultado = avisos.showSnackbar(
            message = "\"${t.titulo}\" completada",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short
        )
        if (resultado == SnackbarResult.ActionPerformed) {
            bd.tareaPlanDao().marcar(t.id, false)
            actualizarWidgets(context)
        }
    }

    fun borrar(t: TareaPlan) = scope.launch {
        bd.tareaPlanDao().borrar(t)
        actualizarWidgets(context)
    }

    // Sirve para las atrasadas y para las sin fecha: les asigna el día de hoy
    fun pasarAHoy(t: TareaPlan) = scope.launch {
        bd.tareaPlanDao().actualizar(t.copy(fecha = hoy, sinFecha = false))
        actualizarWidgets(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (atrasadas.isNotEmpty()) {
                item(key = "atrasadas") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EncabezadoDia(
                            titulo = "Atrasadas",
                            detalle = "${atrasadas.size} pendientes",
                            color = MaterialTheme.colorScheme.error
                        )
                        Grupo {
                            atrasadas.forEachIndexed { i, t ->
                                if (i > 0) Divisor()
                                FilaTareaPlan(
                                    tarea = t,
                                    deadline = t.deadlineId?.let { deadlinePorId[it] },
                                    atrasada = true,
                                    hoy = hoy,
                                    expandida = t.id in expandidas,
                                    onCompletar = { completar(t) },
                                    onEditar = { tareaEditando = t },
                                    onAlternarDescripcion = { alternarDescripcion(t) },
                                    onBorrar = { borrar(t) },
                                    onPasarAHoy = { pasarAHoy(t) }
                                )
                            }
                        }
                    }
                }
            }

            items(dias, key = { "dia-$it" }) { dia ->
                val lista = porDia[dia].orEmpty().sortedBy { it.id }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EncabezadoDia(
                        titulo = tituloDia(dia, hoy),
                        detalle = dia.format(FORMATO_DIA_MES),
                        color = if (dia == hoy) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground
                    )
                    if (lista.isEmpty()) {
                        Text(
                            text = if (dia == hoy && hechasHoy > 0) "Terminaste todo lo de hoy 🎉"
                            else "Nada planeado para hoy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else {
                        Grupo {
                            lista.forEachIndexed { i, t ->
                                if (i > 0) Divisor()
                                FilaTareaPlan(
                                    tarea = t,
                                    deadline = t.deadlineId?.let { deadlinePorId[it] },
                                    atrasada = false,
                                    hoy = hoy,
                                    expandida = t.id in expandidas,
                                    onCompletar = { completar(t) },
                                    onEditar = { tareaEditando = t },
                                    onAlternarDescripcion = { alternarDescripcion(t) },
                                    onBorrar = { borrar(t) },
                                    onPasarAHoy = null
                                )
                            }
                        }
                    }
                }
            }

            // Tareas sin fecha
            if (sinFecha.isNotEmpty()) {
                item(key = "sin-fecha") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EncabezadoDia(
                            titulo = "Sin fecha",
                            detalle = "${sinFecha.size} pendientes",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Grupo {
                            sinFecha.forEachIndexed { i, t ->
                                if (i > 0) Divisor()
                                FilaTareaPlan(
                                    tarea = t,
                                    deadline = null,
                                    atrasada = false,
                                    hoy = hoy,
                                    expandida = t.id in expandidas,
                                    onCompletar = { completar(t) },
                                    onEditar = { tareaEditando = t },
                                    onAlternarDescripcion = { alternarDescripcion(t) },
                                    onBorrar = { borrar(t) },
                                    onPasarAHoy = { pasarAHoy(t) }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "pie") {
                Text(
                    text = "Se muestran los próximos $DIAS_VISIBLES días. Tocá una tarea para editarla o ver su descripción.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        BotonAgregar(
            texto = "Nueva tarea",
            onClick = { mostrandoNueva = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        // Aviso con "Deshacer", encima del botón
        SnackbarHost(
            hostState = avisos,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )
    }

    if (mostrandoNueva) {
        DialogoTarea(
            inicial = null,
            deadlines = deadlinesVinculables,
            onCancelar = { mostrandoNueva = false },
            onGuardar = { nueva ->
                scope.launch {
                    bd.tareaPlanDao().insertar(nueva)
                    actualizarWidgets(context)
                }
                mostrandoNueva = false
            },
            permitirSinFecha = true,
            onGuardarVarias = { nuevas ->
                scope.launch {
                    nuevas.forEach { bd.tareaPlanDao().insertar(it) }
                    actualizarWidgets(context)
                }
                mostrandoNueva = false
            }
        )
    }

    tareaEditando?.let { t ->
        DialogoTarea(
            inicial = t,
            deadlines = deadlinesVinculables,
            onCancelar = { tareaEditando = null },
            onGuardar = { editada ->
                scope.launch {
                    bd.tareaPlanDao().actualizar(editada)
                    actualizarWidgets(context)
                }
                tareaEditando = null
            },
            permitirSinFecha = true
        )
    }
}

// "Hoy", "Mañana" o el nombre del día ("Martes")
private fun tituloDia(dia: LocalDate, hoy: LocalDate): String = when (dia) {
    hoy -> "Hoy"
    hoy.plusDays(1) -> "Mañana"
    else -> dia.format(FORMATO_DIA_SEMANA).replaceFirstChar { it.uppercase() }
}

@Composable
private fun EncabezadoDia(titulo: String, detalle: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge, color = color)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Línea fina entre filas, alineada con el texto (no con la casilla)
@Composable
private fun Divisor() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun FilaTareaPlan(
    tarea: TareaPlan,
    deadline: Deadline?,
    atrasada: Boolean,
    hoy: LocalDate,
    expandida: Boolean,
    onCompletar: () -> Unit,
    onEditar: () -> Unit,
    onAlternarDescripcion: () -> Unit,
    onBorrar: () -> Unit,
    onPasarAHoy: (() -> Unit)?
) {
    val descripcion = tarea.descripcion?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CasillaRedonda(marcada = false, onCambio = onCompletar)

            // Tocar el texto: si tiene descripción la despliega, si no abre la edición
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = if (descripcion != null) onAlternarDescripcion else onEditar)
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tarea.titulo,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Marca de "tiene descripción"
                    if (descripcion != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_descripcion),
                            contentDescription = "Tiene descripción",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (atrasada || deadline != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (atrasada) {
                            Etiqueta(
                                texto = textoDiasRestantes(tarea.fecha, hoy),
                                fondo = MaterialTheme.colorScheme.errorContainer,
                                colorTexto = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (deadline != null) {
                            Etiqueta(
                                texto = deadline.titulo,
                                fondo = MaterialTheme.colorScheme.primaryContainer,
                                colorTexto = MaterialTheme.colorScheme.onPrimaryContainer,
                                icono = R.drawable.ic_deadlines,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = textoDiasRestantes(deadline.fecha, hoy).lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (onPasarAHoy != null) {
                TextButton(
                    onClick = onPasarAHoy,
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_hoy),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hoy")
                }
            }
            IconButton(onClick = onBorrar) {
                Icon(
                    painter = painterResource(R.drawable.ic_borrar),
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Descripción desplegada, alineada con el título
        if (expandida && descripcion != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onEditar,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.offset(x = (-8).dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_editar),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
            }
        }
    }
}