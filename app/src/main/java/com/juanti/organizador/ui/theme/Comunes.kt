@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juanti.organizador.R
import com.juanti.organizador.data.TipoDeadline
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---------- TEXTOS ----------

private val formatoCorto =
    DateTimeFormatter.ofPattern("EEE d 'de' MMM", Locale.forLanguageTag("es-AR"))

// Ejemplo: "Vie 3 de oct."
fun formatoFecha(fecha: LocalDate): String =
    fecha.format(formatoCorto).replaceFirstChar { it.uppercase() }

// Ejemplo: "Hoy", "Mañana", "En 5 días", "Hace 2 días"
fun textoDiasRestantes(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String {
    val dias = ChronoUnit.DAYS.between(hoy, fecha)
    return when {
        dias == 0L -> "Hoy"
        dias == 1L -> "Mañana"
        dias > 1L -> "En $dias días"
        dias == -1L -> "Ayer"
        else -> "Hace ${-dias} días"
    }
}

fun nombreTipo(tipo: TipoDeadline): String = when (tipo) {
    TipoDeadline.EXAMEN -> "Examen"
    TipoDeadline.ENTREGA -> "Entrega"
    TipoDeadline.OTRO -> "Otro"
}

// ---------- PIEZAS VISUALES ----------

// Casilla redonda: vacía con borde, o llena de menta con un ✓
@Composable
fun CasillaRedonda(
    marcada: Boolean,
    onCambio: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    val colorBorde = MaterialTheme.colorScheme.outline
    val colorTilde = MaterialTheme.colorScheme.onTertiary

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onCambio),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (marcada) color else Color.Transparent)
                .border(2.dp, if (marcada) color else colorBorde, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (marcada) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val tilde = Path().apply {
                        moveTo(size.width * 0.12f, size.height * 0.52f)
                        lineTo(size.width * 0.40f, size.height * 0.80f)
                        lineTo(size.width * 0.90f, size.height * 0.22f)
                    }
                    drawPath(
                        path = tilde,
                        color = colorTilde,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

// Etiqueta en forma de píldora, con ícono opcional
@Composable
fun Etiqueta(
    texto: String,
    fondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier,
    icono: Int? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(
                painter = painterResource(icono),
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = colorTexto,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Botón flotante girasol con ícono + texto (ej: "Nueva tarea")
@Composable
fun BotonAgregar(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(painter = painterResource(R.drawable.ic_agregar), contentDescription = null) },
        text = { Text(texto, style = MaterialTheme.typography.labelLarge) }
    )
}

// Bloque redondeado que agrupa varias filas
@Composable
fun Grupo(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        content = contenido
    )
}

// Botón que muestra una fecha y, al tocarlo, abre un calendario para cambiarla
@Composable
fun SelectorFecha(
    fecha: LocalDate,
    onCambio: (LocalDate) -> Unit,
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
        Text(formatoFecha(fecha))
    }

    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        onCambio(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}