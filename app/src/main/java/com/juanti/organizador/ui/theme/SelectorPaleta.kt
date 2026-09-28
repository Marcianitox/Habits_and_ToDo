package com.juanti.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.juanti.organizador.ui.theme.Paleta

// Diálogo para elegir la paleta de colores (se aplica al instante)
@Composable
fun DialogoPaletas(
    actual: Paleta,
    onElegir: (Paleta) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Paleta de colores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Paleta.entries.forEach { p ->
                    OpcionPaleta(
                        paleta = p,
                        seleccionada = p == actual,
                        onClick = { onElegir(p) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Listo") }
        }
    )
}

// Una opción, dibujada con los colores de su propia paleta
@Composable
private fun OpcionPaleta(paleta: Paleta, seleccionada: Boolean, onClick: () -> Unit) {
    val e = paleta.esquema
    val forma = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(e.background)
            .border(
                width = if (seleccionada) 2.dp else 1.dp,
                color = if (seleccionada) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = forma
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Muestras: principal, cumplido, atrasado
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(e.primary, e.tertiary, e.error).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(c)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = paleta.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = e.onBackground
            )
            Text(
                text = if (paleta.oscura) "Oscura" else "Clara",
                style = MaterialTheme.typography.bodySmall,
                color = e.onSurfaceVariant
            )
        }
        if (seleccionada) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = e.primary
            )
        }
    }
}