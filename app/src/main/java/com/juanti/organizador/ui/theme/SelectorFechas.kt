package com.juanti.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juanti.organizador.R
import com.juanti.organizador.data.inicioDeSemana
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale.forLanguageTag("es-AR")
private val FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", ES)
private val FORMATO_DIA_CORTO = DateTimeFormatter.ofPattern("EEE d", ES)

// Botón que muestra los días elegidos y, al tocarlo, abre un calendario
// donde se pueden marcar varios días
@Composable
fun SelectorFechas(
    fechas: Set<LocalDate>,
    onCambio: (Set<LocalDate>) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    FilledTonalButton(onClick = { abierto = true }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_calendario),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = textoFechas(fechas),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (abierto) {
        DialogoCalendarioVarios(
            inicial = fechas,
            onCancelar = { abierto = false },
            onAceptar = {
                onCambio(it)
                abierto = false
            }
        )
    }
}

// "Mar 3 de oct." si es uno solo, "3 días: mar 3, mié 4, jue 5" si son varios
private fun textoFechas(fechas: Set<LocalDate>): String = when (fechas.size) {
    0 -> "Elegir días"
    1 -> formatoFecha(fechas.first())
    else -> "${fechas.size} días: " + fechas.sorted().joinToString(", ") {
        it.format(FORMATO_DIA_CORTO).replace(".", "")
    }
}

@Composable
private fun DialogoCalendarioVarios(
    inicial: Set<LocalDate>,
    onCancelar: () -> Unit,
    onAceptar: (Set<LocalDate>) -> Unit
) {
    val hoy = remember { LocalDate.now() }
    var seleccion by remember { mutableStateOf(inicial) }
    var primeroDeMes by remember { mutableStateOf((inicial.minOrNull() ?: hoy).withDayOfMonth(1)) }

    val ultimoDeMes = primeroDeMes.plusMonths(1).minusDays(1)
    val filas = generateSequence(inicioDeSemana(primeroDeMes)) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(ultimoDeMes) }
        .toList()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Elegí los días") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Navegación ‹ mes ›
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { primeroDeMes = primeroDeMes.minusMonths(1) }) {
                        Icon(painter = painterResource(R.drawable.ic_anterior), contentDescription = "Mes anterior")
                    }
                    Text(
                        text = primeroDeMes.format(FORMATO_MES).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { primeroDeMes = primeroDeMes.plusMonths(1) }) {
                        Icon(painter = painterResource(R.drawable.ic_siguiente), contentDescription = "Mes siguiente")
                    }
                }

                // Letras de los días
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { d ->
                        Text(
                            text = d.getDisplayName(TextStyle.NARROW, ES).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Días
                Column {
                    filas.forEach { inicioFila ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (i in 0L..6L) {
                                val dia = inicioFila.plusDays(i)
                                CeldaElegible(
                                    dia = dia,
                                    visible = !dia.isBefore(primeroDeMes) && !dia.isAfter(ultimoDeMes),
                                    elegido = dia in seleccion,
                                    esHoy = dia == hoy,
                                    onTocar = {
                                        seleccion = if (dia in seleccion) seleccion - dia else seleccion + dia
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = when (seleccion.size) {
                        0 -> "Tocá los días en los que tenés que hacerla"
                        1 -> "1 día elegido"
                        else -> "${seleccion.size} días elegidos: se crea una tarea por día"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = seleccion.isNotEmpty(),
                onClick = { onAceptar(seleccion) }
            ) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun CeldaElegible(
    dia: LocalDate,
    visible: Boolean,
    elegido: Boolean,
    esHoy: Boolean,
    onTocar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (elegido) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .then(
                        if (esHoy && !elegido) {
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else Modifier
                    )
                    .clickable { onTocar() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dia.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (elegido || esHoy) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}