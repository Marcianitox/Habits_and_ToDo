@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juanti.organizador.R
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.Habito
import com.juanti.organizador.data.RegistroHabito
import com.juanti.organizador.data.TipoFrecuencia
import com.juanti.organizador.data.apareceHoy
import com.juanti.organizador.data.textoFrecuencia
import com.juanti.organizador.data.textoRacha
import com.juanti.organizador.data.tocaEl
import com.juanti.organizador.data.vecesEnSemana
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale.forLanguageTag("es-AR")
private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", ES)

// Desde cuándo leer registros (en la práctica: todo el historial)
private val REGISTROS_DESDE: LocalDate = LocalDate.of(2000, 1, 1)

@Composable
fun PantallaHabitos() {
    val context = LocalContext.current
    val dao = remember { BaseDeDatos.obtener(context).habitoDao() }
    val hoy = remember { LocalDate.now() }

    val flujoHabitos = remember { dao.activos() }
    val flujoRegistros = remember { dao.registrosEntre(REGISTROS_DESDE, hoy) }
    val habitos by flujoHabitos.collectAsState(initial = emptyList())
    val registros by flujoRegistros.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var mostrandoNuevo by remember { mutableStateOf(false) }
    var habitoEditando by remember { mutableStateOf<Habito?>(null) }
    var aBorrar by remember { mutableStateOf<Habito?>(null) }

    // Para cada hábito, el conjunto de días en que se cumplió
    val hechosPorHabito: Map<Long, Set<LocalDate>> =
        registros.groupBy({ it.habitoId }, { it.fecha }).mapValues { it.value.toSet() }

    fun alternar(h: Habito, dia: LocalDate) = scope.launch {
        val registro = RegistroHabito(habitoId = h.id, fecha = dia)
        if (dia in hechosPorHabito[h.id].orEmpty()) dao.desregistrar(registro)
        else dao.registrar(registro)
        actualizarWidgets(context)
    }

    // Los de hoy: primero los pendientes, después los hechos
    val deHoy = habitos
        .filter { it.apareceHoy(hoy, hechosPorHabito[it.id].orEmpty()) }
        .sortedBy { hoy in hechosPorHabito[it.id].orEmpty() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (habitos.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Todavía no tenés hábitos",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Creá el primero con el botón de abajo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hoy
                item(key = "hoy") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EncabezadoSeccion(
                            titulo = "Hoy",
                            detalle = hoy.format(FORMATO_DIA_MES),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (deHoy.isEmpty()) {
                            Text(
                                text = "Hoy no te toca ningún hábito.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else {
                            Grupo {
                                deHoy.forEachIndexed { i, h ->
                                    if (i > 0) Divisor()
                                    val hechos = hechosPorHabito[h.id].orEmpty()
                                    FilaHabitoHoy(
                                        habito = h,
                                        hecho = hoy in hechos,
                                        hechos = hechos,
                                        hoy = hoy,
                                        onAlternar = { alternar(h, hoy) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Mis hábitos
                item(key = "titulo-todos") {
                    EncabezadoSeccion(
                        titulo = "Mis hábitos",
                        detalle = "${habitos.size}",
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                items(habitos, key = { "todos${it.id}" }) { h ->
                    TarjetaHabito(
                        habito = h,
                        hechos = hechosPorHabito[h.id].orEmpty(),
                        hoy = hoy,
                        onAlternar = { dia -> alternar(h, dia) },
                        onEditar = { habitoEditando = h },
                        onBorrar = { aBorrar = h }
                    )
                }
            }
        }

        BotonAgregar(
            texto = "Nuevo hábito",
            onClick = { mostrandoNuevo = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (mostrandoNuevo) {
        DialogoHabito(
            inicial = null,
            onCancelar = { mostrandoNuevo = false },
            onGuardar = { nuevo ->
                scope.launch {
                    dao.insertar(nuevo)
                    actualizarWidgets(context)
                }
                mostrandoNuevo = false
            }
        )
    }

    habitoEditando?.let { h ->
        DialogoHabito(
            inicial = h,
            onCancelar = { habitoEditando = null },
            onGuardar = { editado ->
                scope.launch {
                    dao.actualizar(editado)
                    actualizarWidgets(context)
                }
                habitoEditando = null
            }
        )
    }

    aBorrar?.let { h ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text("¿Borrar hábito?") },
            text = { Text("Se va a borrar \"${h.nombre}\" junto con todo su historial.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.borrar(h)
                        actualizarWidgets(context)
                    }
                    aBorrar = null
                }) { Text("Borrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { aBorrar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EncabezadoSeccion(
    titulo: String,
    detalle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 4.dp)
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

// Línea fina entre filas, alineada con el texto
@Composable
private fun Divisor() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun FilaHabitoHoy(
    habito: Habito,
    hecho: Boolean,
    hechos: Set<LocalDate>,
    hoy: LocalDate,
    onAlternar: () -> Unit
) {
    val semanal = habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA

    Row(
        modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CasillaRedonda(marcada = hecho, onCambio = onAlternar)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habito.nombre,
                style = MaterialTheme.typography.bodyLarge,
                color = if (hecho) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = habito.textoFrecuencia(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (semanal) {
            Etiqueta(
                texto = "${habito.vecesEnSemana(hoy, hechos)}/${habito.vecesPorSemana} esta semana",
                fondo = MaterialTheme.colorScheme.secondaryContainer,
                colorTexto = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            Etiqueta(
                texto = habito.textoRacha(hoy, hechos),
                fondo = MaterialTheme.colorScheme.primaryContainer,
                colorTexto = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TarjetaHabito(
    habito: Habito,
    hechos: Set<LocalDate>,
    hoy: LocalDate,
    onAlternar: (LocalDate) -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    Grupo {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = habito.nombre, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = habito.textoFrecuencia(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Etiqueta(
                            texto = habito.textoRacha(hoy, hechos),
                            fondo = MaterialTheme.colorScheme.primaryContainer,
                            colorTexto = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(onClick = onEditar) {
                    Icon(
                        painter = painterResource(R.drawable.ic_editar),
                        contentDescription = "Editar hábito",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onBorrar) {
                    Icon(
                        painter = painterResource(R.drawable.ic_borrar),
                        contentDescription = "Borrar hábito",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(modifier = Modifier.padding(end = 12.dp)) {
                TiraUltimosDias(habito = habito, hechos = hechos, hoy = hoy, onAlternar = onAlternar)
            }
        }
    }
}

// Los últimos 7 días como círculos que se pueden tocar
@Composable
private fun TiraUltimosDias(
    habito: Habito,
    hechos: Set<LocalDate>,
    hoy: LocalDate,
    onAlternar: (LocalDate) -> Unit
) {
    val semanal = habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 6 downTo 0) {
            val dia = hoy.minusDays(i.toLong())
            val esHoy = dia == hoy
            val hecho = dia in hechos
            val tocaba = habito.tocaEl(dia)
            val falto = tocaba && !hecho && !semanal && dia.isBefore(hoy)
            val pendienteHoy = esHoy && tocaba && !hecho

            val fondo = if (hecho) MaterialTheme.colorScheme.tertiary else Color.Transparent
            val borde: Color? = when {
                falto -> MaterialTheme.colorScheme.error
                pendienteHoy -> MaterialTheme.colorScheme.primary
                tocaba && !hecho -> MaterialTheme.colorScheme.outlineVariant
                else -> null
            }
            val colorTexto = when {
                hecho -> MaterialTheme.colorScheme.onTertiary
                falto -> MaterialTheme.colorScheme.error
                tocaba -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.outline
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dia.dayOfWeek.getDisplayName(TextStyle.NARROW, ES).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Medium,
                    color = if (esHoy) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(fondo)
                        .then(
                            if (borde != null) {
                                Modifier.border(if (pendienteHoy) 2.dp else 1.5.dp, borde, CircleShape)
                            } else Modifier
                        )
                        .clickable(enabled = tocaba || hecho) { onAlternar(dia) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dia.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (hecho || esHoy) FontWeight.SemiBold else FontWeight.Normal,
                        color = colorTexto
                    )
                }
            }
        }
    }
}

private fun nombreFrecuencia(tipo: TipoFrecuencia): String = when (tipo) {
    TipoFrecuencia.DIARIA -> "Todos los días"
    TipoFrecuencia.CADA_N_DIAS -> "Cada N días"
    TipoFrecuencia.DIAS_SEMANA -> "Días fijos"
    TipoFrecuencia.VECES_POR_SEMANA -> "X por semana"
}

// Crear (inicial = null) o editar un hábito
@Composable
private fun DialogoHabito(
    inicial: Habito?,
    onCancelar: () -> Unit,
    onGuardar: (Habito) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipoFrecuencia ?: TipoFrecuencia.DIARIA) }
    var cadaN by remember { mutableIntStateOf((inicial?.cadaNDias ?: 2).coerceAtLeast(2)) }
    var veces by remember { mutableIntStateOf(inicial?.vecesPorSemana ?: 3) }
    var dias by remember { mutableStateOf(inicial?.diasSemana ?: setOf()) }
    var inicio by remember { mutableStateOf(inicial?.fechaInicio ?: LocalDate.now()) }

    val valido = nombre.isNotBlank() && (tipo != TipoFrecuencia.DIAS_SEMANA || dias.isNotEmpty())

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nuevo hábito" else "Editar hábito") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Ir al gimnasio)") },
                    singleLine = true
                )

                Text(
                    text = "Frecuencia",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipoFrecuencia.entries.forEach { t ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            label = { Text(nombreFrecuencia(t)) }
                        )
                    }
                }

                when (tipo) {
                    TipoFrecuencia.DIARIA -> {}
                    TipoFrecuencia.CADA_N_DIAS ->
                        Contador(antes = "Cada", valor = cadaN, despues = "días", min = 2, max = 30) { cadaN = it }
                    TipoFrecuencia.DIAS_SEMANA ->
                        SelectorDias(seleccion = dias) { dias = it }
                    TipoFrecuencia.VECES_POR_SEMANA ->
                        Contador(antes = "", valor = veces, despues = "veces por semana", min = 1, max = 7) { veces = it }
                }

                Text(
                    text = "Empezó el",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectorFecha(fecha = inicio, onCambio = { inicio = it })

                if (inicial != null) {
                    Text(
                        text = "Tu historial se conserva. Si cambiás la frecuencia o la fecha de inicio, " +
                                "la racha y los porcentajes se recalculan con la configuración nueva.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valido,
                onClick = {
                    val base = inicial ?: Habito(nombre = "")
                    onGuardar(
                        base.copy(
                            nombre = nombre.trim(),
                            tipoFrecuencia = tipo,
                            cadaNDias = cadaN,
                            diasSemana = dias,
                            vecesPorSemana = veces,
                            fechaInicio = inicio
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

// Selector numérico:  Cada ( − ) 2 ( + ) días
@Composable
private fun Contador(
    antes: String,
    valor: Int,
    despues: String,
    min: Int,
    max: Int,
    onCambio: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (antes.isNotEmpty()) {
            Text(antes, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(10.dp))
        }
        FilledTonalIconButton(
            onClick = { onCambio((valor - 1).coerceAtLeast(min)) },
            enabled = valor > min
        ) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text(
            text = "$valor",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        FilledTonalIconButton(
            onClick = { onCambio((valor + 1).coerceAtMost(max)) },
            enabled = valor < max
        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        Spacer(modifier = Modifier.width(10.dp))
        Text(despues, style = MaterialTheme.typography.bodyLarge)
    }
}

// Los 7 días de la semana como círculos para elegir
@Composable
private fun SelectorDias(seleccion: Set<DayOfWeek>, onCambio: (Set<DayOfWeek>) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DayOfWeek.entries.forEach { d ->
            val elegido = d in seleccion
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (elegido) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .clickable { onCambio(if (elegido) seleccion - d else seleccion + d) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = d.getDisplayName(TextStyle.NARROW, ES).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (elegido) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}