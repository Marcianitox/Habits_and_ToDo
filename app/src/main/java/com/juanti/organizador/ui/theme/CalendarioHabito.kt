@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.juanti.organizador.data.inicioDeSemana
import com.juanti.organizador.data.proporcion
import com.juanti.organizador.data.tocaEl
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale.forLanguageTag("es-AR")
private val FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", ES)
private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMM", ES)

enum class VistaCalendario(val etiqueta: String) {
    SEMANA("Semana"),
    MES("Mes")
}

@Composable
fun CalendarioHabito(habito: Habito, hechos: Set<LocalDate>, hoy: LocalDate) {
    val context = LocalContext.current
    val dao = remember { BaseDeDatos.obtener(context).habitoDao() }
    val scope = rememberCoroutineScope()

    var vista by rememberSaveable { mutableStateOf(VistaCalendario.SEMANA) }
    var lunes by rememberSaveable { mutableStateOf(inicioDeSemana(hoy)) }
    var primeroDeMes by rememberSaveable { mutableStateOf(hoy.withDayOfMonth(1)) }

    // Marcar / desmarcar un día
    fun alternar(dia: LocalDate) = scope.launch {
        val registro = RegistroHabito(habitoId = habito.id, fecha = dia)
        if (dia in hechos) dao.desregistrar(registro) else dao.registrar(registro)
        actualizarWidgets(context)
    }

    val (desde, hasta) = when (vista) {
        VistaCalendario.SEMANA -> lunes to lunes.plusDays(6)
        VistaCalendario.MES -> primeroDeMes to primeroDeMes.plusMonths(1).minusDays(1)
    }
    val titulo = when (vista) {
        VistaCalendario.SEMANA -> textoSemana(lunes)
        VistaCalendario.MES -> primeroDeMes.format(FORMATO_MES).replaceFirstChar { it.uppercase() }
    }
    val hayAnterior = desde.isAfter(habito.fechaInicio)
    val haySiguiente = hasta.isBefore(hoy)
    val semanal = habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA

    // Filas del calendario: una por semana (lunes de cada una)
    val filas = generateSequence(inicioDeSemana(desde)) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(hasta) }
        .toList()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Título + selector Semana | Mes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Calendario",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            SelectorSegmentado(
                opciones = VistaCalendario.entries.map { it.etiqueta },
                seleccion = vista.ordinal,
                onSeleccion = { vista = VistaCalendario.entries[it] },
                modifier = Modifier.width(180.dp)
            )
        }

        // Navegación ‹ período ›
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                enabled = hayAnterior,
                onClick = {
                    if (vista == VistaCalendario.SEMANA) lunes = lunes.minusWeeks(1)
                    else primeroDeMes = primeroDeMes.minusMonths(1)
                }
            ) {
                Icon(painter = painterResource(R.drawable.ic_anterior), contentDescription = "Anterior")
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                enabled = haySiguiente,
                onClick = {
                    if (vista == VistaCalendario.SEMANA) lunes = lunes.plusWeeks(1)
                    else primeroDeMes = primeroDeMes.plusMonths(1)
                }
            ) {
                Icon(painter = painterResource(R.drawable.ic_siguiente), contentDescription = "Siguiente")
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
                        CeldaDia(
                            dia = dia,
                            habito = habito,
                            hechos = hechos,
                            hoy = hoy,
                            visible = !dia.isBefore(desde) && !dia.isAfter(hasta),
                            onTocar = { alternar(dia) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Resumen del período mostrado
        val hastaEfectivo = if (hasta.isAfter(hoy)) hoy else hasta
        val p = habito.proporcion(hechos, desde, hastaEfectivo)
        val unidad = if (semanal) "veces" else "días"
        Text(
            text = if (p.esperados == 0) "Sin datos en este período"
            else "Cumplido ${p.hechos} de ${p.esperados} $unidad",
            style = MaterialTheme.typography.bodyMedium
        )

        Leyenda(habito = habito)
        Text(
            text = "Tocá un día para marcarlo o desmarcarlo.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CeldaDia(
    dia: LocalDate,
    habito: Habito,
    hechos: Set<LocalDate>,
    hoy: LocalDate,
    visible: Boolean,
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
            val semanal = habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA
            val esHoy = dia == hoy
            val hecho = dia in hechos
            val futuro = dia.isAfter(hoy)
            val tocaba = habito.tocaEl(dia)
            val falto = tocaba && !hecho && !semanal && dia.isBefore(hoy)
            val pendienteHoy = esHoy && tocaba && !hecho
            val sePuedeTocar = !futuro && (tocaba || hecho)

            val fondo = if (hecho) MaterialTheme.colorScheme.tertiary else Color.Transparent
            val borde: Color? = when {
                falto -> MaterialTheme.colorScheme.error
                pendienteHoy -> MaterialTheme.colorScheme.primary
                else -> null
            }
            val colorTexto = when {
                hecho -> MaterialTheme.colorScheme.onTertiary
                falto -> MaterialTheme.colorScheme.error
                tocaba && !futuro -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.outline
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(fondo)
                    .then(
                        if (borde != null) {
                            Modifier.border(if (pendienteHoy) 2.dp else 1.5.dp, borde, CircleShape)
                        } else Modifier
                    )
                    .clickable(enabled = sePuedeTocar) { onTocar() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dia.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto,
                    fontWeight = if (hecho || esHoy) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun Leyenda(habito: Habito) {
    val semanal = habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA
    val tieneDiasSinTocar = habito.tipoFrecuencia == TipoFrecuencia.DIAS_SEMANA ||
            habito.tipoFrecuencia == TipoFrecuencia.CADA_N_DIAS

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemLeyenda(fondo = MaterialTheme.colorScheme.tertiary, borde = null, texto = "Cumplido")
        if (!semanal) {
            ItemLeyenda(fondo = Color.Transparent, borde = MaterialTheme.colorScheme.error, texto = "Faltó")
        }
        ItemLeyenda(fondo = Color.Transparent, borde = MaterialTheme.colorScheme.primary, texto = "Hoy")
        if (tieneDiasSinTocar) {
            Text(
                text = "Gris: no tocaba",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ItemLeyenda(fondo: Color, borde: Color?, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(fondo)
                .then(
                    if (borde != null) Modifier.border(1.5.dp, borde, CircleShape)
                    else Modifier
                )
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Ej: "21 – 27 de sep."  o  "29 de sep. – 5 de oct."
private fun textoSemana(lunes: LocalDate): String {
    val domingo = lunes.plusDays(6)
    return if (lunes.month == domingo.month) {
        "${lunes.dayOfMonth} – ${domingo.format(FORMATO_DIA_MES)}"
    } else {
        "${lunes.format(FORMATO_DIA_MES)} – ${domingo.format(FORMATO_DIA_MES)}"
    }
}