package com.juanti.organizador.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

// Refresca todos los widgets de la app.
suspend fun actualizarWidgets(context: Context) {
    WidgetPlanDelDia().updateAll(context)
    WidgetHabitos().updateAll(context)
    WidgetDeadlines().updateAll(context)
    WidgetGraficoHabitos().updateAll(context)
}