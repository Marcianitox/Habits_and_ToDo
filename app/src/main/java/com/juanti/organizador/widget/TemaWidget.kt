package com.juanti.organizador.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import com.juanti.organizador.MainActivity
import com.juanti.organizador.R
import com.juanti.organizador.ui.Seccion
import com.juanti.organizador.ui.theme.leerPaleta

// Colores de los widgets: los de la paleta que el usuario eligió en la app
fun coloresWidget(context: Context): ColorProviders {
    val esquema = leerPaleta(context).esquema
    return ColorProviders(light = esquema, dark = esquema)
}

// Claves que viajan con los botones de los widgets
val CLAVE_ID = ActionParameters.Key<Long>("id")
val CLAVE_TIPO = ActionParameters.Key<String>("tipo")
val CLAVE_SECCION = ActionParameters.Key<String>("seccion")

// Qué formulario abre la ventanita de carga rápida
const val TIPO_TAREA = "tarea"
const val TIPO_DEADLINE = "deadline"

// Acción que abre la app directamente en una sección
fun abrirSeccion(seccion: Seccion): Action =
    actionStartActivity<MainActivity>(actionParametersOf(CLAVE_SECCION to seccion.name))

// Casilla redonda de los widgets: vacía (borde) o llena con ✓ (color "cumplido")
@Composable
fun CasillaWidget(marcada: Boolean, tamano: Dp = 30.dp) {
    if (marcada) {
        Box(
            modifier = GlanceModifier
                .size(tamano)
                .cornerRadius(tamano / 2)
                .background(GlanceTheme.colors.tertiary),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_tilde),
                contentDescription = null,
                modifier = GlanceModifier.size(tamano * 0.7f),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiary)
            )
        }
    } else {
        Image(
            provider = ImageProvider(R.drawable.widget_casilla_vacia),
            contentDescription = null,
            modifier = GlanceModifier.size(tamano),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.outline)
        )
    }
}