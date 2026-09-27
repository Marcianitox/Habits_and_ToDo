package com.juanti.organizador.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.juanti.organizador.MainActivity
import com.juanti.organizador.R
import com.juanti.organizador.data.Habito
import com.juanti.organizador.data.BaseDeDatos
import com.juanti.organizador.data.TipoFrecuencia
import com.juanti.organizador.data.evolucionSemanal
import com.juanti.organizador.data.inicioDeSemana
import com.juanti.organizador.data.tocaEl
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as EstiloFecha
import java.util.Locale
import kotlin.math.roundToInt

private val ES = Locale.forLanguageTag("es-AR")
private val FORMATO_MES = DateTimeFormatter.ofPattern("MMMM", ES)
private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMM", ES)

// A partir de esta altura aparece el calendario del mes
private val ALTO_PARA_MES = 300.dp

// Widget "Hábitos: semana y calendario"
class WidgetGraficoHabitos : GlanceAppWidget() {

    // Se redibuja según el tamaño real del widget (para mostrar o no el mes)
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = BaseDeDatos.obtener(context).habitoDao()
        val hoy = LocalDate.now()
        val lunes = inicioDeSemana(hoy)
        val primeroDeMes = hoy.withDayOfMonth(1)
        val desde = if (lunes.isBefore(primeroDeMes)) lunes else primeroDeMes

        val flujoHabitos = dao.activos()
        val flujoRegistros = dao.registrosEntre(desde, hoy)
        val inicialHabitos = flujoHabitos.first()
        val inicialRegistros = flujoRegistros.first()

        provideContent {
            val habitos by flujoHabitos.collectAsState(initial = inicialHabitos)
            val registros by flujoRegistros.collectAsState(initial = inicialRegistros)
            val hechosPorHabito: Map<Long, Set<LocalDate>> =
                registros.groupBy({ it.habitoId }, { it.fecha }).mapValues { it.value.toSet() }

            GlanceTheme(colors = ColoresWidget) {
                ContenidoGrafico(
                    hoy = hoy,
                    habitos = habitos,
                    hechosPorHabito = hechosPorHabito,
                    mostrarMes = LocalSize.current.height >= ALTO_PARA_MES
                )
            }
        }
    }
}

@Composable
private fun ContenidoGrafico(
    hoy: LocalDate,
    habitos: List<Habito>,
    hechosPorHabito: Map<Long, Set<LocalDate>>,
    mostrarMes: Boolean
) {
    val lunes = inicioDeSemana(hoy)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp)
            .padding(14.dp)
    ) {
        // Encabezado (abre la app)
        Column(modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>())) {
            Text(
                text = "Esta semana",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = textoSemana(lunes),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (habitos.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Creá tus hábitos en la app",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                item(itemId = Long.MAX_VALUE) {
                    EncabezadoDias(lunes = lunes, hoy = hoy)
                }
                items(habitos, itemId = { it.id }) { h ->
                    FilaSemana(
                        habito = h,
                        hechos = hechosPorHabito[h.id].orEmpty(),
                        lunes = lunes,
                        hoy = hoy
                    )
                }
                if (mostrarMes) {
                    item(itemId = Long.MAX_VALUE - 1) {
                        CalendarioMes(hoy = hoy, habitos = habitos, hechosPorHabito = hechosPorHabito)
                    }
                }
            }
        }
    }
}

// Fila de letras: (nombre)  L M X J V S D  %
@Composable
private fun EncabezadoDias(lunes: LocalDate, hoy: LocalDate) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "", modifier = GlanceModifier.defaultWeight())
        for (i in 0L..6L) {
            val dia = lunes.plusDays(i)
            Text(
                text = letraDia(dia.dayOfWeek),
                modifier = GlanceModifier.width(26.dp),
                style = TextStyle(
                    color = if (dia == hoy) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (dia == hoy) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
        }
        Text(
            text = "%",
            modifier = GlanceModifier.width(40.dp),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.End
            )
        )
    }
}

// Un hábito: nombre, los 7 días de la semana y el porcentaje
@Composable
private fun FilaSemana(habito: Habito, hechos: Set<LocalDate>, lunes: LocalDate, hoy: LocalDate) {
    val fraccion = habito.evolucionSemanal(hechos, hoy, semanas = 1).first().second
    val colorPorcentaje = when {
        fraccion == null -> GlanceTheme.colors.onSurfaceVariant
        fraccion >= 0.8f -> GlanceTheme.colors.tertiary
        fraccion >= 0.5f -> GlanceTheme.colors.primary
        else -> GlanceTheme.colors.error
    }

    // Column exterior = separación entre filas
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habito.nombre,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            for (i in 0L..6L) {
                val dia = lunes.plusDays(i)
                Box(
                    modifier = GlanceModifier.width(26.dp).height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(iconoDia(habito, dia, hoy, hechos)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }
            Text(
                text = fraccion?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                modifier = GlanceModifier.width(40.dp),
                style = TextStyle(
                    color = colorPorcentaje,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            )
        }
    }
}

// Qué símbolo lleva cada día de la tabla semanal
private fun iconoDia(habito: Habito, dia: LocalDate, hoy: LocalDate, hechos: Set<LocalDate>): Int = when {
    dia in hechos -> R.drawable.widget_casilla_tildada
    !habito.tocaEl(dia) -> R.drawable.widget_dia_libre
    dia.isAfter(hoy) -> R.drawable.widget_casilla_vacia
    dia == hoy -> R.drawable.widget_dia_hoy
    habito.tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA -> R.drawable.widget_casilla_vacia
    else -> R.drawable.widget_dia_falto
}

// Calendario del mes: todos los hábitos combinados
@Composable
private fun CalendarioMes(
    hoy: LocalDate,
    habitos: List<Habito>,
    hechosPorHabito: Map<Long, Set<LocalDate>>
) {
    val primero = hoy.withDayOfMonth(1)
    val ultimo = primero.plusMonths(1).minusDays(1)
    val filas = generateSequence(inicioDeSemana(primero)) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(ultimo) }
        .toList()

    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = primero.format(FORMATO_MES).replaceFirstChar { it.uppercase() },
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
        // Letras de los días
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    text = letraDia(d),
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
        // Semanas del mes
        filas.forEach { lunesFila ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                for (i in 0L..6L) {
                    val dia = lunesFila.plusDays(i)
                    CeldaMes(
                        dia = dia,
                        visible = dia.month == primero.month,
                        hoy = hoy,
                        habitos = habitos,
                        hechosPorHabito = hechosPorHabito
                    )
                }
            }
        }
        Leyenda()
    }
}

@Composable
private fun androidx.glance.layout.RowScope.CeldaMes(
    dia: LocalDate,
    visible: Boolean,
    hoy: LocalDate,
    habitos: List<Habito>,
    hechosPorHabito: Map<Long, Set<LocalDate>>
) {
    Box(
        modifier = GlanceModifier.defaultWeight().height(30.dp).padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            // Solo cuentan los hábitos con días definidos (no los de "X por semana")
            val programados = habitos.filter {
                it.tipoFrecuencia != TipoFrecuencia.VECES_POR_SEMANA && it.tocaEl(dia)
            }
            val cumplidos = programados.count { dia in hechosPorHabito[it.id].orEmpty() }

            val fondo: ColorProvider?
            val colorTexto: ColorProvider
            when {
                dia.isAfter(hoy) -> {
                    fondo = null
                    colorTexto = GlanceTheme.colors.outline
                }
                programados.isEmpty() -> {
                    fondo = null
                    colorTexto = GlanceTheme.colors.onSurfaceVariant
                }
                cumplidos == programados.size -> {
                    fondo = GlanceTheme.colors.tertiary
                    colorTexto = GlanceTheme.colors.onTertiary
                }
                cumplidos > 0 -> {
                    fondo = GlanceTheme.colors.tertiaryContainer
                    colorTexto = GlanceTheme.colors.onTertiaryContainer
                }
                dia == hoy -> {
                    fondo = GlanceTheme.colors.surfaceVariant
                    colorTexto = GlanceTheme.colors.primary
                }
                else -> {
                    fondo = GlanceTheme.colors.errorContainer
                    colorTexto = GlanceTheme.colors.onErrorContainer
                }
            }

            var modificador = GlanceModifier.fillMaxSize().cornerRadius(8.dp)
            if (fondo != null) modificador = modificador.background(fondo)

            Box(modifier = modificador, contentAlignment = Alignment.Center) {
                Text(
                    text = dia.dayOfMonth.toString(),
                    style = TextStyle(
                        color = colorTexto,
                        fontSize = 11.sp,
                        fontWeight = if (dia == hoy) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun Leyenda() {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = GlanceModifier.size(10.dp).cornerRadius(3.dp).background(GlanceTheme.colors.tertiary)) {}
        Text(
            text = "Todo",
            modifier = GlanceModifier.padding(start = 4.dp, end = 10.dp),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
        )
        Box(modifier = GlanceModifier.size(10.dp).cornerRadius(3.dp).background(GlanceTheme.colors.tertiaryContainer)) {}
        Text(
            text = "Una parte",
            modifier = GlanceModifier.padding(start = 4.dp, end = 10.dp),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
        )
        Box(modifier = GlanceModifier.size(10.dp).cornerRadius(3.dp).background(GlanceTheme.colors.errorContainer)) {}
        Text(
            text = "Nada",
            modifier = GlanceModifier.padding(start = 4.dp),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
        )
    }
}

private fun letraDia(d: DayOfWeek): String =
    d.getDisplayName(EstiloFecha.NARROW, ES).uppercase()

// Ej: "21 – 27 de sep."  o  "29 de sep. – 5 de oct."
private fun textoSemana(lunes: LocalDate): String {
    val domingo = lunes.plusDays(6)
    return if (lunes.month == domingo.month) {
        "${lunes.dayOfMonth} – ${domingo.format(FORMATO_DIA_MES)}"
    } else {
        "${lunes.format(FORMATO_DIA_MES)} – ${domingo.format(FORMATO_DIA_MES)}"
    }
}

class ReceptorGraficoHabitos : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WidgetGraficoHabitos()
}
