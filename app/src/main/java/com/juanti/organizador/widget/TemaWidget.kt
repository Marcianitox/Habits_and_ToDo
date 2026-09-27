package com.juanti.organizador.widget

import androidx.compose.material3.darkColorScheme
import androidx.glance.action.ActionParameters
import androidx.glance.material3.ColorProviders
import com.juanti.organizador.ui.theme.Contorno
import com.juanti.organizador.ui.theme.Coral
import com.juanti.organizador.ui.theme.CoralContenedor
import com.juanti.organizador.ui.theme.Girasol
import com.juanti.organizador.ui.theme.GirasolContenedor
import com.juanti.organizador.ui.theme.Menta
import com.juanti.organizador.ui.theme.MentaContenedor
import com.juanti.organizador.ui.theme.SobreCoral
import com.juanti.organizador.ui.theme.SobreCoralContenedor
import com.juanti.organizador.ui.theme.SobreGirasol
import com.juanti.organizador.ui.theme.SobreGirasolContenedor
import com.juanti.organizador.ui.theme.SobreMenta
import com.juanti.organizador.ui.theme.SobreMentaContenedor
import com.juanti.organizador.ui.theme.Texto
import com.juanti.organizador.ui.theme.TextoSuave
import com.juanti.organizador.ui.theme.TintaClara
import com.juanti.organizador.ui.theme.TintaElevada
import com.juanti.organizador.ui.theme.TintaFondo
import com.juanti.organizador.ui.theme.TintaSeleccion

// Colores "Tinta y girasol" para los widgets (siempre oscuros, igual que la app)
private val EsquemaWidget = darkColorScheme(
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
    outline = Contorno
)

val ColoresWidget = ColorProviders(light = EsquemaWidget, dark = EsquemaWidget)

// Claves que viajan con los botones de los widgets
val CLAVE_ID = ActionParameters.Key<Long>("id")
val CLAVE_TIPO = ActionParameters.Key<String>("tipo")

// Qué formulario abre la ventanita de carga rápida
const val TIPO_TAREA = "tarea"
const val TIPO_DEADLINE = "deadline"