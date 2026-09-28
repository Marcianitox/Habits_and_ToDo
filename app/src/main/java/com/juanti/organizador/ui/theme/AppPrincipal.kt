package com.juanti.organizador.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.juanti.organizador.R
import com.juanti.organizador.recordatorios.reprogramarRecordatorios
import com.juanti.organizador.ui.theme.Paleta
import com.juanti.organizador.widget.actualizarWidgets
import kotlinx.coroutines.launch

// Las cuatro secciones de la barra de abajo
enum class Seccion(val titulo: String, val icono: Int) {
    PLAN("Plan", R.drawable.ic_plan),
    DEADLINES("Deadlines", R.drawable.ic_deadlines),
    HABITOS("Hábitos", R.drawable.ic_habitos),
    PROGRESO("Progreso", R.drawable.ic_progreso)
}

@Composable
fun AppPrincipal(
    seccionPedida: Seccion? = null,
    onSeccionAbierta: () -> Unit = {},
    paleta: Paleta = Paleta.TINTA,
    onCambiarPaleta: (Paleta) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seccion by rememberSaveable { mutableStateOf(Seccion.PLAN) }
    var eligiendoPaleta by remember { mutableStateOf(false) }

    // Si un widget pidió una sección, ir a esa
    LaunchedEffect(seccionPedida) {
        if (seccionPedida != null) {
            seccion = seccionPedida
            onSeccionAbierta()
        }
    }

    // Cada vez que se toca "refrescar", este número cambia y obliga a las
    // pantallas a recalcular todo desde cero (incluido el día de hoy)
    var refresco by remember { mutableIntStateOf(0) }

    // Pedir permiso de notificaciones (Android 13 o más nuevo) y programar recordatorios
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        reprogramarRecordatorios(context)
    }

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
            // Título grande de la sección + paleta + refrescar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = seccion.titulo,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { eligiendoPaleta = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_paleta),
                        contentDescription = "Paleta de colores",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    refresco++
                    scope.launch { actualizarWidgets(context) }
                    Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refrescar),
                        contentDescription = "Refrescar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                key(refresco) {
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

    if (eligiendoPaleta) {
        DialogoPaletas(
            actual = paleta,
            onElegir = { nueva ->
                onCambiarPaleta(nueva)
                scope.launch { actualizarWidgets(context) }
            },
            onCerrar = { eligiendoPaleta = false }
        )
    }
}