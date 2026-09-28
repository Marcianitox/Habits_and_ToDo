package com.juanti.organizador

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.juanti.organizador.ui.AppPrincipal
import com.juanti.organizador.ui.Seccion
import com.juanti.organizador.ui.theme.HabitsAndToDoTheme
import com.juanti.organizador.ui.theme.Paleta
import com.juanti.organizador.ui.theme.guardarPaleta
import com.juanti.organizador.ui.theme.leerPaleta

class MainActivity : ComponentActivity() {

    // Sección que pidió abrir un widget (se usa una vez y se borra)
    private var seccionPedida by mutableStateOf<Seccion?>(null)

    // Paleta de colores elegida
    private var paleta by mutableStateOf(Paleta.TINTA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paleta = leerPaleta(this)
        aplicarBarras(paleta)

        // Solo al abrir de cero (no al girar la pantalla)
        if (savedInstanceState == null) {
            seccionPedida = leerSeccion(intent)
        }

        setContent {
            HabitsAndToDoTheme(paleta = paleta) {
                AppPrincipal(
                    seccionPedida = seccionPedida,
                    onSeccionAbierta = { seccionPedida = null },
                    paleta = paleta,
                    onCambiarPaleta = { nueva ->
                        paleta = nueva
                        guardarPaleta(this, nueva)
                        aplicarBarras(nueva)
                    }
                )
            }
        }
    }

    // Si la app ya estaba abierta y se toca un widget, llega por acá
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        seccionPedida = leerSeccion(intent)
    }

    // Íconos de la barra de estado: claros en paletas oscuras, oscuros en paletas claras
    private fun aplicarBarras(p: Paleta) {
        val estilo = if (p.oscura) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
    }

    private fun leerSeccion(intent: Intent?): Seccion? {
        val nombre = intent?.getStringExtra("seccion") ?: return null
        return Seccion.entries.find { it.name == nombre }
    }
}