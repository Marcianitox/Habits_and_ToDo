package com.juanti.organizador.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.juanti.organizador.R

// Las cuatro secciones de la barra de abajo
enum class Seccion(val titulo: String, val icono: Int) {
    PLAN("Plan", R.drawable.ic_plan),
    DEADLINES("Deadlines", R.drawable.ic_deadlines),
    HABITOS("Hábitos", R.drawable.ic_habitos),
    PROGRESO("Progreso", R.drawable.ic_progreso)
}

@Composable
fun AppPrincipal() {
    var seccion by rememberSaveable { mutableStateOf(Seccion.PLAN) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Seccion.entries.forEach { s ->
                    NavigationBarItem(
                        selected = seccion == s,
                        onClick = { seccion = s },
                        icon = {
                            Icon(
                                painter = painterResource(s.icono),
                                contentDescription = s.titulo
                            )
                        },
                        label = { Text(s.titulo, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Título grande de la sección
            Text(
                text = seccion.titulo,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (seccion) {
                    Seccion.PLAN -> PantallaPlan()
                    Seccion.DEADLINES -> PantallaDeadlines()
                    Seccion.HABITOS -> PantallaHabitos()
                    Seccion.PROGRESO -> PantallaProgreso()
                }
            }
        }
    }
}