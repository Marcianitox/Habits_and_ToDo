@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juanti.organizador.data.Deadline
import com.juanti.organizador.data.TareaPlan
import com.juanti.organizador.data.TipoDeadline
import java.time.LocalDate

// Opciones de recordatorio: días antes → texto
private val OPCIONES_RECORDATORIO = listOf(
    0 to "El mismo día",
    1 to "1 día antes",
    3 to "3 días antes",
    7 to "1 semana antes"
)

// Minutos desde las 00:00 → "20:00"
fun textoHora(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)

// Crear (inicial = null) o editar una deadline
@Composable
fun DialogoDeadline(
    inicial: Deadline?,
    onCancelar: () -> Unit,
    onGuardar: (Deadline) -> Unit
) {
    var titulo by remember { mutableStateOf(inicial?.titulo ?: "") }
    var materia by remember { mutableStateOf(inicial?.materia ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: TipoDeadline.EXAMEN) }
    var fecha by remember { mutableStateOf(inicial?.fecha ?: LocalDate.now().plusDays(7)) }
    // Las deadlines nuevas vienen sin recordatorios
    var recordatorios by remember { mutableStateOf(inicial?.recordatorios ?: emptySet()) }
    var hora by remember { mutableIntStateOf(inicial?.horaRecordatorio ?: (20 * 60)) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nueva deadline" else "Editar deadline") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = materia,
                    onValueChange = { materia = it },
                    label = { Text("Materia (opcional)") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoDeadline.entries.forEach { t ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            label = { Text(nombreTipo(t)) }
                        )
                    }
                }
                SelectorFecha(fecha = fecha, onCambio = { fecha = it })

                // Recordatorios (opcionales)
                Text(
                    text = "Recordatorios",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = recordatorios.isEmpty(),
                        onClick = { recordatorios = emptySet() },
                        label = { Text("Sin recordatorio") }
                    )
                    OPCIONES_RECORDATORIO.forEach { (dias, texto) ->
                        val elegido = dias in recordatorios
                        FilterChip(
                            selected = elegido,
                            onClick = {
                                recordatorios = if (elegido) recordatorios - dias else recordatorios + dias
                            },
                            label = { Text(texto) }
                        )
                    }
                }
                if (recordatorios.isNotEmpty()) {
                    SelectorHora(minutos = hora, onCambio = { hora = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = titulo.isNotBlank(),
                onClick = {
                    val base = inicial ?: Deadline(titulo = "", fecha = fecha)
                    onGuardar(
                        base.copy(
                            titulo = titulo.trim(),
                            materia = materia.trim().ifBlank { null },
                            tipo = tipo,
                            fecha = fecha,
                            recordatorios = recordatorios,
                            horaRecordatorio = hora
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

// Botón con la hora; al tocarlo se puede escribir otra
@Composable
private fun SelectorHora(minutos: Int, onCambio: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }

    FilledTonalButton(onClick = { abierto = true }) {
        Text("A las ${textoHora(minutos)}")
    }

    if (abierto) {
        val estado = rememberTimePickerState(
            initialHour = minutos / 60,
            initialMinute = minutos % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text("Hora del recordatorio") },
            text = { TimeInput(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onCambio(estado.hour * 60 + estado.minute)
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text("Cancelar") }
            }
        )
    }
}

// Crear (inicial = null) o editar una tarea del plan.
// Si "deadlines" no está vacía, se puede elegir a cuál vincularla.
@Composable
fun DialogoTarea(
    inicial: TareaPlan?,
    deadlines: List<Deadline>,
    onCancelar: () -> Unit,
    onGuardar: (TareaPlan) -> Unit
) {
    var titulo by remember { mutableStateOf(inicial?.titulo ?: "") }
    var fecha by remember { mutableStateOf(inicial?.fecha ?: LocalDate.now()) }
    var deadlineId by remember { mutableStateOf(inicial?.deadlineId) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nueva tarea" else "Editar tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Qué tenés que hacer") }
                )
                SelectorFecha(fecha = fecha, onCambio = { fecha = it })

                if (deadlines.isNotEmpty()) {
                    Text(
                        text = "Vincular a una deadline (opcional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = deadlineId == null,
                            onClick = { deadlineId = null },
                            label = { Text("Ninguna") }
                        )
                        deadlines.forEach { d ->
                            FilterChip(
                                selected = deadlineId == d.id,
                                onClick = { deadlineId = d.id },
                                label = { Text(d.titulo) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = titulo.isNotBlank(),
                onClick = {
                    val base = inicial ?: TareaPlan(titulo = "", fecha = fecha)
                    onGuardar(base.copy(titulo = titulo.trim(), fecha = fecha, deadlineId = deadlineId))
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}