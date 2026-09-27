package com.juanti.organizador.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val ES = Locale.forLanguageTag("es-AR")

// Lunes de la semana de esa fecha
fun inicioDeSemana(fecha: LocalDate): LocalDate =
    fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

// ¿Ese día le "toca" al hábito?
// En "X veces por semana" cualquier día sirve.
fun Habito.tocaEl(fecha: LocalDate): Boolean {
    if (fecha.isBefore(fechaInicio)) return false
    return when (tipoFrecuencia) {
        TipoFrecuencia.DIARIA -> true
        TipoFrecuencia.CADA_N_DIAS ->
            ChronoUnit.DAYS.between(fechaInicio, fecha) % cadaNDias.coerceAtLeast(1) == 0L
        TipoFrecuencia.DIAS_SEMANA -> fecha.dayOfWeek in diasSemana
        TipoFrecuencia.VECES_POR_SEMANA -> true
    }
}

// Cuántas veces se cumplió en la semana (lunes a domingo) de esa fecha
fun Habito.vecesEnSemana(fecha: LocalDate, hechos: Set<LocalDate>): Int {
    val lunes = inicioDeSemana(fecha)
    return (0L..6L).count { lunes.plusDays(it) in hechos }
}

// ¿Aparece en la sección "Hoy"?
fun Habito.apareceHoy(hoy: LocalDate, hechos: Set<LocalDate>): Boolean =
    if (tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) {
        !hoy.isBefore(fechaInicio) &&
                (hoy in hechos || vecesEnSemana(hoy, hechos) < vecesPorSemana)
    } else {
        tocaEl(hoy)
    }

// Racha actual: días seguidos (o semanas seguidas, en "X veces por semana")
// Hoy solo suma si ya está hecho; si no, todavía hay tiempo y no corta la racha.
fun Habito.racha(hoy: LocalDate, hechos: Set<LocalDate>): Int {
    if (tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) {
        val primera = inicioDeSemana(fechaInicio)
        var semana = inicioDeSemana(hoy)
        var cuenta = 0
        if (vecesEnSemana(semana, hechos) >= vecesPorSemana) cuenta++
        semana = semana.minusWeeks(1)
        while (!semana.isBefore(primera) && vecesEnSemana(semana, hechos) >= vecesPorSemana) {
            cuenta++
            semana = semana.minusWeeks(1)
        }
        return cuenta
    }

    var cuenta = 0
    if (tocaEl(hoy) && hoy in hechos) cuenta++
    var dia = hoy.minusDays(1)
    while (!dia.isBefore(fechaInicio)) {
        if (tocaEl(dia)) {
            if (dia in hechos) cuenta++ else break
        }
        dia = dia.minusDays(1)
    }
    return cuenta
}

// Texto legible de la frecuencia. Ej: "Día de por medio", "lun, mié, vie"
fun Habito.textoFrecuencia(): String = when (tipoFrecuencia) {
    TipoFrecuencia.DIARIA -> "Todos los días"
    TipoFrecuencia.CADA_N_DIAS ->
        if (cadaNDias == 2) "Día de por medio" else "Cada $cadaNDias días"
    TipoFrecuencia.DIAS_SEMANA ->
        diasSemana.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, ES) }
    TipoFrecuencia.VECES_POR_SEMANA ->
        if (vecesPorSemana == 1) "1 vez por semana" else "$vecesPorSemana veces por semana"
}

fun Habito.textoRacha(hoy: LocalDate, hechos: Set<LocalDate>): String {
    val r = racha(hoy, hechos)
    val unidad = if (tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) {
        if (r == 1) "semana" else "semanas"
    } else {
        if (r == 1) "día" else "días"
    }
    return "🔥 $r $unidad"
}
