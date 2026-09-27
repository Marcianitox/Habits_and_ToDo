package com.juanti.organizador.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EsquemaTintaYGirasol = darkColorScheme(
    primary = Girasol,
    onPrimary = SobreGirasol,
    primaryContainer = GirasolContenedor,
    onPrimaryContainer = SobreGirasolContenedor,

    secondary = TintaClara,
    onSecondary = TintaFondo,
    secondaryContainer = TintaSeleccion,
    onSecondaryContainer = Texto,

    tertiary = Menta,
    onTertiary = SobreMenta,
    tertiaryContainer = MentaContenedor,
    onTertiaryContainer = SobreMentaContenedor,

    error = Coral,
    onError = SobreCoral,
    errorContainer = CoralContenedor,
    onErrorContainer = SobreCoralContenedor,

    background = TintaFondo,
    onBackground = Texto,
    surface = TintaFondo,
    onSurface = Texto,
    surfaceVariant = TintaElevada,
    onSurfaceVariant = TextoSuave,
    surfaceTint = TintaElevada,

    surfaceContainerLowest = TintaProfunda,
    surfaceContainerLow = TintaBaja,
    surfaceContainer = TintaBarra,
    surfaceContainerHigh = TintaElevada,
    surfaceContainerHighest = TintaTarjeta,

    outline = Contorno,
    outlineVariant = ContornoSuave
)

// Bordes redondeados
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Siempre oscuro y con colores propios (sin color dinámico del fondo de pantalla)
@Composable
fun HabitsAndToDoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaTintaYGirasol,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}