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

class MainActivity : ComponentActivity() {

    // Sección que pidió abrir un widget (se usa una vez y se borra)
    private var seccionPedida by mutableStateOf<Seccion?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras del sistema transparentes con íconos claros (la app es siempre oscura)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // Solo al abrir de cero (no al girar la pantalla)
        if (savedInstanceState == null) {
            seccionPedida = leerSeccion(intent)
        }
        setContent {
            HabitsAndToDoTheme {
                AppPrincipal(
                    seccionPedida = seccionPedida,
                    onSeccionAbierta = { seccionPedida = null }
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

    private fun leerSeccion(intent: Intent?): Seccion? {
        val nombre = intent?.getStringExtra("seccion") ?: return null
        return Seccion.entries.find { it.name == nombre }
    }
}