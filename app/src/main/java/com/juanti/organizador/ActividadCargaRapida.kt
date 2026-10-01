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
import androidx.compose.foundation.verticalScroll
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
import com.juanti.organizador.ui.DialogoDeadline
import com.juanti.organizador.ui.SelectorFechas
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
                    TIPO_DEADLINE -> DialogoDeadline(
                        inicial = null,
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
                        onGuardar = { tareas ->
                            guardar { bd -> tareas.forEach { bd.tareaPlanDao().insertar(it) } }
                        }
                    )
                }
            }
        }
    }

    // Guarda, refresca widgets y recordatorios, y cierra la ventanita
    private fun guardar(accion: suspend (BaseDeDatos) -> Unit) {
        lifecycleScope.launch {
            accion(BaseDeDatos.obtener(applicationContext))
            actualizarWidgets(applicationContext)
            finish()
        }
    }
}

@Composable
private fun FormularioTarea(onCancelar: () -> Unit, onGuardar: (List<TareaPlan>) -> Unit) {
    val context = LocalContext.current
    val hoy = remember { LocalDate.now() }
    val flujo = remember { BaseDeDatos.obtener(context).deadlineDao().pendientesConPlan(hoy) }
    val deadlines by flujo.collectAsState(initial = emptyList())

    var titulo by remember { mutableStateOf("") }
    var fechas by remember { mutableStateOf(setOf(hoy)) }
    var deadlineId by remember { mutableStateOf<Long?>(null) }
    var descripcion by remember { mutableStateOf("") }
    val foco = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva tarea") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Qué tenés que hacer") },
                    modifier = Modifier.focusRequester(foco)
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    maxLines = 6
                )
                // Abre el teclado directamente
                LaunchedEffect(Unit) {
                    delay(150)
                    runCatching { foco.requestFocus() }
                }
                SelectorFechas(fechas = fechas, onCambio = { fechas = it })

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
                enabled = titulo.isNotBlank() && fechas.isNotEmpty(),
                onClick = {
                    // Una tarea independiente por cada día elegido
                    onGuardar(
                        fechas.sorted().map { dia ->
                            TareaPlan(
                                titulo = titulo.trim(),
                                fecha = dia,
                                deadlineId = deadlineId,
                                descripcion = descripcion.trim().ifBlank { null }
                            )
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