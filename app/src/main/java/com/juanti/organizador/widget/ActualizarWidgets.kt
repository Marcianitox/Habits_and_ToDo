package com.juanti.organizador.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.juanti.organizador.recordatorios.reprogramarRecordatorios

// Se llama después de cualquier cambio en los datos:
// refresca todos los widgets y reprograma los recordatorios.
suspend fun actualizarWidgets(context: Context) {
    WidgetPlanDelDia().updateAll(context)
    WidgetHabitos().updateAll(context)
    WidgetDeadlines().updateAll(context)
    WidgetGraficoHabitos().updateAll(context)
    reprogramarRecordatorios(context)
}