package com.juanti.organizador.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Bordes redondeados
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Tema de la app con la paleta elegida.
// Si no se indica una paleta, usa la que el usuario dejó guardada.
@Composable
fun HabitsAndToDoTheme(
    paleta: Paleta? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val elegida = paleta ?: remember { leerPaleta(context) }

    MaterialTheme(
        colorScheme = elegida.esquema,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}