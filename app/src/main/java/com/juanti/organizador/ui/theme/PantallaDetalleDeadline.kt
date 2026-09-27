@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.juanti.organizador.R
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.Deadline
import com.juanti.organizador.data.TareaPlan
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun PantallaDetalleDeadline(deadlineId: Long, onVolver: () -> Unit) {
    val context = LocalContext.current
    val bd = remember { BaseDeDatos.obtener(context) }
    val flujo = remember(deadlineId) { bd.deadlineDao().conPlan(deadlineId) }
    val datos by flujo.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var mostrandoNueva by remember { mutableStateOf(false) }
    var editandoDeadline by remember { mutableStateOf(false) }
    var sesionEditando by remember { mutableStateOf<TareaPlan?>(null) }
    var confirmandoBorrado by remember { mutableStateOf(false) }

    // El botón "atrás" del celular vuelve a la lista
    BackHandler(onBack = onVolver)

    val actual = datos ?: return
    val d = actual.deadline
    val tareas = actual.tareas.sortedWith(compareBy({ it.fecha }, { it.id }))
    val hechas = tareas.count { it.completada }
    val hoy = LocalDate.now()
    val vencida = d.fecha.isBefore(hoy)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Volver, editar y borrar
            item(key = "barra") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) {
                        Icon(painter = painterResource(R.drawable.ic_volver), contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { editandoDeadline = true }) {
                        Icon(painter = painterResource(R.drawable.ic_editar), contentDescription = "Editar deadline")
                    }
                    IconButton(onClick = { confirmandoBorrado = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_borrar),
                            contentDescription = "Borrar deadline",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Título, tipo, materia y fecha
            item(key = "cabecera") {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = d.titulo, style = MaterialTheme.typography.headlineMedium)
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = formatoFecha(d.fecha), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (vencida) "venció ${textoDiasRestantes(d.fecha, hoy).lowercase()}"
                            else textoDiasRestantes(d.fecha, hoy).lowercase(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (vencida) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Resumen del avance
            item(key = "resumen") {
                Grupo {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Plan de acción",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (tareas.isEmpty()) "Sin sesiones"
                                else "$hechas de ${tareas.size} hechas",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (tareas.isNotEmpty()) {
                            BarraProgreso(
                                fraccion = hechas.toFloat() / tareas.size,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        FilledTonalButton(onClick = {
                            scope.launch {
                                bd.deadlineDao().actualizar(d.copy(completada = true))
                                actualizarWidgets(context)
                                onVolver()
                            }
                        }) {
                            Text("Marcar deadline como hecha")
                        }
                    }
                }
            }

            // Sesiones
            item(key = "titulo-sesiones") {
                Column(modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
                    Text(text = "Sesiones", style = MaterialTheme.typography.titleLarge)
                    if (tareas.isNotEmpty()) {
                        Text(
                            text = "Tocá una sesión para editarla.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (tareas.isEmpty()) {
                item(key = "sin-sesiones") {
                    Text(
                        text = "Todavía no hay sesiones. Agregá la primera con el botón de abajo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                item(key = "sesiones") {
                    Grupo {
                        tareas.forEachIndexed { i, t ->
                            if (i > 0) Divisor()
                            FilaSesion(
                                tarea = t,
                                hoy = hoy,
                                onMarcar = {
                                    scope.launch {
                                        bd.tareaPlanDao().marcar(t.id, !t.completada)
                                        actualizarWidgets(context)
                                    }
                                },
                                onEditar = { sesionEditando = t },
                                onBorrar = {
                                    scope.launch {
                                        bd.tareaPlanDao().borrar(t)
                                        actualizarWidgets(context)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        BotonAgregar(
            texto = "Nueva sesión",
            onClick = { mostrandoNueva = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (mostrandoNueva) {
        DialogoNuevaSesion(
            deadline = d,
            onCancelar = { mostrandoNueva = false },
            onGuardar = { nuevas ->
                scope.launch {
                    nuevas.forEach { bd.tareaPlanDao().insertar(it) }
                    actualizarWidgets(context)
                }
                mostrandoNueva = false
            }
        )
    }

    if (editandoDeadline) {
        DialogoDeadline(
            inicial = d,
            onCancelar = { editandoDeadline = false },
            onGuardar = { editada ->
                scope.launch {
                    bd.deadlineDao().actualizar(editada)
                    actualizarWidgets(context)
                }
                editandoDeadline = false
            }
        )
    }

    sesionEditando?.let { t ->
        DialogoTarea(
            inicial = t,
            deadlines = emptyList(),
            onCancelar = { sesionEditando = null },
            onGuardar = { editada ->
                scope.launch {
                    bd.tareaPlanDao().actualizar(editada)
                    actualizarWidgets(context)
                }
                sesionEditando = null
            }
        )
    }

    if (confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = false },
            title = { Text("¿Borrar deadline?") },
            text = { Text("Se va a borrar \"${d.titulo}\" junto con todas las sesiones de su plan de acción.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoBorrado = false
                    scope.launch {
                        bd.deadlineDao().borrar(d)
                        actualizarWidgets(context)
                        onVolver()
                    }
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = false }) { Text("Cancelar") }
            }
        )
    }
}

// Línea fina entre filas, alineada con el texto
@Composable
private fun Divisor() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun FilaSesion(
    tarea: TareaPlan,
    hoy: LocalDate,
    onMarcar: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    val atrasada = !tarea.completada && tarea.fecha.isBefore(hoy)

    Row(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CasillaRedonda(marcada = tarea.completada, onCambio = onMarcar)

        // Tocar el texto abre la edición
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditar)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = tarea.titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tarea.completada) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (tarea.completada) TextDecoration.LineThrough else null
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = formatoFecha(tarea.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (atrasada) {
                    Etiqueta(
                        texto = textoDiasRestantes(tarea.fecha, hoy),
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        colorTexto = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
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
}

@Composable
private fun DialogoNuevaSesion(
    deadline: Deadline,
    onCancelar: () -> Unit,
    onGuardar: (List<TareaPlan>) -> Unit
) {
    val hoy = remember { LocalDate.now() }
    var titulo by remember { mutableStateOf("Estudiar para ${deadline.titulo}") }
    var fecha by remember { mutableStateOf(if (deadline.fecha.isAfter(hoy)) hoy else deadline.fecha) }
    var repetir by remember { mutableStateOf(false) }

    val ultimoDia = deadline.fecha.minusDays(1)
    val rangoValido = !fecha.isAfter(ultimoDia)
    val cantidad = if (rangoValido) ChronoUnit.DAYS.between(fecha, ultimoDia) + 1 else 0

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Qué vas a hacer") }
                )
                Text(
                    text = if (repetir) "Desde" else "Fecha",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectorFecha(fecha = fecha, onCambio = { fecha = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = repetir, onCheckedChange = { repetir = it })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Todos los días hasta el día anterior a la deadline",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (repetir) {
                    Text(
                        text = if (rangoValido) "Se van a crear $cantidad sesiones"
                        else "La fecha de inicio tiene que ser anterior al día de la deadline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rangoValido) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = titulo.isNotBlank() && (!repetir || rangoValido),
                onClick = {
                    val fechas = if (repetir) {
                        generateSequence(fecha) { it.plusDays(1) }
                            .takeWhile { !it.isAfter(ultimoDia) }
                            .toList()
                    } else {
                        listOf(fecha)
                    }
                    onGuardar(
                        fechas.map {
                            TareaPlan(titulo = titulo.trim(), fecha = it, deadlineId = deadline.id)
                        }
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}