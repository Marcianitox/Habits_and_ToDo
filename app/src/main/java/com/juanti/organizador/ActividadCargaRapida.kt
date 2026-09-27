@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.Deadline
import com.juanti.organizador.data.TareaPlan
import com.juanti.organizador.data.TipoDeadline
import com.juanti.organizador.ui.SelectorFecha
import com.juanti.organizador.ui.nombreTipo
import com.juanti.organizador.ui.theme.HabitsAndToDoTheme
import com.juanti.organizador.widget.TIPO_COMPLETAR_DEADLINE
import com.juanti.organizador.widget.TIPO_DEADLINE
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

// Ventanita flotante para cargar o confirmar algo desde un widget, sin abrir la app completa
class ActividadCargaRapida : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tipo = intent.getStringExtra("tipo")
        val id = intent.getLongExtra("id", -1L)

        setContent {
            HabitsAndToDoTheme {
                when (tipo) {
                    TIPO_DEADLINE -> FormularioDeadline(
                        onCancelar = { finish() },
                        onGuardar = { d -> guardar { it.deadlineDao().insertar(d) } }
                    )
                    TIPO_COMPLETAR_DEADLINE -> ConfirmarCompletarDeadline(
                        id = id,
                        onCancelar = { finish() },
                        onConfirmar = { d ->
                            guardar { it.deadlineDao().actualizar(d.copy(completada = true)) }
                        }
                    )
                    else -> FormularioTarea(
                        onCancelar = { finish() },
                        onGuardar = { t -> guardar { it.tareaPlanDao().insertar(t) } }
                    )
                }
            }
        }
    }

    // Guarda, refresca los widgets y cierra la ventanita
    private fun guardar(accion: suspend (BaseDeDatos) -> Unit) {
        lifecycleScope.launch {
            accion(BaseDeDatos.obtener(applicationContext))
            actualizarWidgets(applicationContext)
            finish()
        }
    }
}

@Composable
private fun FormularioTarea(onCancelar: () -> Unit, onGuardar: (TareaPlan) -> Unit) {
    val context = LocalContext.current
    val hoy = remember { LocalDate.now() }
    val flujo = remember { BaseDeDatos.obtener(context).deadlineDao().pendientesConPlan(hoy) }
    val deadlines by flujo.collectAsState(initial = emptyList())

    var titulo by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(hoy) }
    var deadlineId by remember { mutableStateOf<Long?>(null) }
    val foco = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Qué tenés que hacer") },
                    modifier = Modifier.focusRequester(foco)
                )
                // Abre el teclado directamente
                LaunchedEffect(Unit) {
                    delay(150)
                    runCatching { foco.requestFocus() }
                }
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
                        deadlines.forEach { dcp ->
                            val d = dcp.deadline
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
                    onGuardar(TareaPlan(titulo = titulo.trim(), fecha = fecha, deadlineId = deadlineId))
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun FormularioDeadline(onCancelar: () -> Unit, onGuardar: (Deadline) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var materia by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoDeadline.EXAMEN) }
    var fecha by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    val foco = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva deadline") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.focusRequester(foco)
                )
                LaunchedEffect(Unit) {
                    delay(150)
                    runCatching { foco.requestFocus() }
                }
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
                    onGuardar(
                        Deadline(
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

// Confirmación antes de marcar una deadline como completada
@Composable
private fun ConfirmarCompletarDeadline(
    id: Long,
    onCancelar: () -> Unit,
    onConfirmar: (Deadline) -> Unit
) {
    val context = LocalContext.current
    val deadline by produceState<Deadline?>(initialValue = null, id) {
        value = BaseDeDatos.obtener(context).deadlineDao().conPlan(id).first()?.deadline
        // Si no existe (por ejemplo, se borró), cerramos la ventanita
        if (value == null) onCancelar()
    }
    val d = deadline ?: return

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Completar deadline?") },
        text = {
            Text("\"${d.titulo}\" se va a marcar como completada y va a dejar de aparecer en tus pendientes.")
        },
        confirmButton = {
            Button(onClick = { onConfirmar(d) }) { Text("Completar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}