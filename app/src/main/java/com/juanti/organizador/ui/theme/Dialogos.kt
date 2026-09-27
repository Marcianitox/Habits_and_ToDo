@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juanti.organizador.data.Deadline
import com.juanti.organizador.data.TareaPlan
import com.juanti.organizador.data.TipoDeadline
import java.time.LocalDate

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

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nueva deadline" else "Editar deadline") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            fecha = fecha
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