package com.juanti.organizador.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------- 1. TINTA Y GIRASOL (oscura, la original) ----------

private val EsquemaTinta = darkColorScheme(
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

// ---------- 2. DÍA (clara, misma identidad) ----------

private val EsquemaDia = lightColorScheme(
    primary = Color(0xFFB87F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE7AE),
    onPrimaryContainer = Color(0xFF3D2A00),
    secondary = Color(0xFF3F5285),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE3F7),
    onSecondaryContainer = Color(0xFF1B2540),
    tertiary = Color(0xFF2E9C84),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCDEFE5),
    onTertiaryContainer = Color(0xFF00382C),
    error = Color(0xFFD9534F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFADAD8),
    onErrorContainer = Color(0xFF5C1210),
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF1B2540),
    surface = Color(0xFFEEF1F8),
    onSurface = Color(0xFF1B2540),
    surfaceVariant = Color(0xFFE3E8F3),
    onSurfaceVariant = Color(0xFF5B6782),
    surfaceTint = Color(0xFFE3E8F3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FD),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFF8A94AD),
    outlineVariant = Color(0xFFD5DBE8)
)

// ---------- 3. ALGODÓN (clara, pastel) ----------

private val EsquemaAlgodon = lightColorScheme(
    primary = Color(0xFF8B76D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE3D9),       // durazno suave (rachas)
    onPrimaryContainer = Color(0xFF6B2E1A),
    secondary = Color(0xFF7A6FA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E0F7),
    onSecondaryContainer = Color(0xFF2D2640),
    tertiary = Color(0xFF5FB896),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3F1E5),
    onTertiaryContainer = Color(0xFF0F4A36),
    error = Color(0xFFE0708F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE0E8),
    onErrorContainer = Color(0xFF6A1830),
    background = Color(0xFFFAF6FF),
    onBackground = Color(0xFF2D2640),
    surface = Color(0xFFF2ECFA),
    onSurface = Color(0xFF2D2640),
    surfaceVariant = Color(0xFFE9E1F5),
    onSurfaceVariant = Color(0xFF6B6283),
    surfaceTint = Color(0xFFE9E1F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFBFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFFA79FBF),
    outlineVariant = Color(0xFFE4DCF0)
)

// ---------- 4. LAVANDA NOCTURNA (oscura) ----------

private val EsquemaLavanda = darkColorScheme(
    primary = Color(0xFFB9A6FF),
    onPrimary = Color(0xFF24185A),
    primaryContainer = Color(0xFF5E3B2C),       // durazno oscuro (rachas)
    onPrimaryContainer = Color(0xFFFFD8C4),
    secondary = Color(0xFFC9C2E8),
    onSecondary = Color(0xFF17152A),
    secondaryContainer = Color(0xFF3B3663),
    onSecondaryContainer = Color(0xFFEDEBF7),
    tertiary = Color(0xFF7FD6B4),
    onTertiary = Color(0xFF0A3325),
    tertiaryContainer = Color(0xFF1E4A3C),
    onTertiaryContainer = Color(0xFFBDF2DE),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF4A0E0E),
    errorContainer = Color(0xFF5C2330),
    onErrorContainer = Color(0xFFFFD9DE),
    background = Color(0xFF17152A),
    onBackground = Color(0xFFEDEBF7),
    surface = Color(0xFF17152A),
    onSurface = Color(0xFFEDEBF7),
    surfaceVariant = Color(0xFF2E2A4D),
    onSurfaceVariant = Color(0xFFADA7C9),
    surfaceTint = Color(0xFF2E2A4D),
    surfaceContainerLowest = Color(0xFF110F20),
    surfaceContainerLow = Color(0xFF1A1830),
    surfaceContainer = Color(0xFF1C1A33),
    surfaceContainerHigh = Color(0xFF2E2A4D),
    surfaceContainerHighest = Color(0xFF221F3B),
    outline = Color(0xFF6E6891),
    outlineVariant = Color(0xFF34304F)
)

// ---------- LAS CUATRO PALETAS ----------

enum class Paleta(val nombre: String, val oscura: Boolean, val esquema: ColorScheme) {
    TINTA("Tinta y girasol", true, EsquemaTinta),
    DIA("Día", false, EsquemaDia),
    ALGODON("Algodón", false, EsquemaAlgodon),
    LAVANDA("Lavanda nocturna", true, EsquemaLavanda)
}

// ---------- GUARDAR Y LEER LA ELECCIÓN ----------

private const val ARCHIVO_PREFERENCIAS = "preferencias"
private const val CLAVE_PALETA = "paleta"

fun leerPaleta(context: Context): Paleta {
    val nombre = context
        .getSharedPreferences(ARCHIVO_PREFERENCIAS, Context.MODE_PRIVATE)
        .getString(CLAVE_PALETA, null)
    return Paleta.entries.find { it.name == nombre } ?: Paleta.TINTA
}

fun guardarPaleta(context: Context, paleta: Paleta) {
    context
        .getSharedPreferences(ARCHIVO_PREFERENCIAS, Context.MODE_PRIVATE)
        .edit()
        .putString(CLAVE_PALETA, paleta.name)
        .apply()
}
