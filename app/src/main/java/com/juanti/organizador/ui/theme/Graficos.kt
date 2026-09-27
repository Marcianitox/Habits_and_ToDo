@file:OptIn(ExperimentalMaterial3Api::class)

package com.juanti.organizador.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Anillo de porcentaje con el número en el centro
@Composable
fun Anillo(
    fraccion: Float?,
    modifier: Modifier = Modifier,
    tamano: Dp = 116.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val colorFondo = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = size.minDimension * 0.11f
            val margen = grosor / 2f
            val tamanoArco = Size(size.width - grosor, size.height - grosor)
            drawArc(
                color = colorFondo,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(margen, margen),
                size = tamanoArco,
                style = Stroke(width = grosor)
            )
            if (fraccion != null && fraccion > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraccion.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(margen, margen),
                    size = tamanoArco,
                    style = Stroke(width = grosor, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = if (fraccion == null) "—" else "${(fraccion * 100).roundToInt()}%",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

// Gráfico de línea de 0% a 100% con degradé debajo.
// El último tramo va punteado y el último punto hueco (período en curso).
@Composable
fun GraficoLinea(
    valores: List<Float?>,
    etiquetas: List<String>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val colorGrilla = MaterialTheme.colorScheme.outlineVariant
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val colorHueco = MaterialTheme.colorScheme.surfaceContainerHighest
    val medidor = rememberTextMeasurer()
    val estilo = TextStyle(fontSize = 10.sp, color = colorTexto)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val n = valores.size
        if (n == 0) return@Canvas

        val margenIzq = 36.dp.toPx()
        val margenDer = 12.dp.toPx()
        val arriba = 8.dp.toPx()
        val altoEtiquetas = 18.dp.toPx()
        val altoArea = size.height - arriba - altoEtiquetas
        val anchoArea = size.width - margenIzq - margenDer
        val base = arriba + altoArea

        fun xDe(i: Int): Float =
            if (n == 1) margenIzq + anchoArea / 2f else margenIzq + anchoArea * i / (n - 1)

        fun yDe(v: Float): Float = arriba + altoArea * (1f - v.coerceIn(0f, 1f))

        // Líneas guía: 0%, 50%, 100%
        listOf(0f, 0.5f, 1f).forEach { f ->
            val y = yDe(f)
            drawLine(
                color = colorGrilla,
                start = Offset(margenIzq, y),
                end = Offset(size.width - margenDer, y),
                strokeWidth = 1.dp.toPx()
            )
            val medida = medidor.measure("${(f * 100).roundToInt()}%", estilo)
            drawText(
                textLayoutResult = medida,
                topLeft = Offset(margenIzq - medida.size.width - 6.dp.toPx(), y - medida.size.height / 2f)
            )
        }

        // Degradé debajo de la línea
        val relleno = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)),
            startY = arriba,
            endY = base
        )
        for (i in 1 until n) {
            val a = valores[i - 1]
            val b = valores[i]
            if (a != null && b != null) {
                val tramo = Path().apply {
                    moveTo(xDe(i - 1), yDe(a))
                    lineTo(xDe(i), yDe(b))
                    lineTo(xDe(i), base)
                    lineTo(xDe(i - 1), base)
                    close()
                }
                drawPath(path = tramo, brush = relleno)
            }
        }

        // Línea
        val grosor = 3.dp.toPx()
        for (i in 1 until n) {
            val a = valores[i - 1]
            val b = valores[i]
            if (a != null && b != null) {
                drawLine(
                    color = color,
                    start = Offset(xDe(i - 1), yDe(a)),
                    end = Offset(xDe(i), yDe(b)),
                    strokeWidth = grosor,
                    cap = StrokeCap.Round,
                    pathEffect = if (i == n - 1)
                        androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                    else null
                )
            }
        }

        // Puntos (el último, hueco)
        valores.forEachIndexed { i, v ->
            if (v != null) {
                val centro = Offset(xDe(i), yDe(v))
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = centro)
                if (i == n - 1) {
                    drawCircle(color = colorHueco, radius = 2.2.dp.toPx(), center = centro)
                }
            }
        }

        // Etiquetas de abajo
        etiquetas.forEachIndexed { i, e ->
            if (e.isNotEmpty()) {
                val medida = medidor.measure(e, estilo)
                drawText(
                    textLayoutResult = medida,
                    topLeft = Offset(xDe(i) - medida.size.width / 2f, size.height - altoEtiquetas + 4.dp.toPx())
                )
            }
        }
    }
}

// Selector segmentado (ej: 7 días | 30 días | 90 días)
@Composable
fun SelectorSegmentado(
    opciones: List<String>,
    seleccion: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        opciones.forEachIndexed { i, texto ->
            SegmentedButton(
                selected = i == seleccion,
                onClick = { onSeleccion(i) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                icon = {}
            ) {
                Text(texto, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}