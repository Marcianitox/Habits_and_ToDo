package com.juanti.organizador.data

import java.time.LocalDate

// "hechos de esperados", ej: 7 de 10
data class Proporcion(val hechos: Int, val esperados: Int) {
    val fraccion: Float?
        get() = if (esperados == 0) null else hechos.toFloat() / esperados

    operator fun plus(otra: Proporcion) =
        Proporcion(hechos + otra.hechos, esperados + otra.esperados)

    companion object {
        val VACIA = Proporcion(0, 0)
    }
}

// Tareas del plan hechas / planificadas entre dos fechas
fun proporcionTareas(tareas: List<TareaPlan>, desde: LocalDate, hasta: LocalDate): Proporcion {
    val enRango = tareas.filter { !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }
    return Proporcion(enRango.count { it.completada }, enRango.size)
}

// Cumplimiento de un hábito entre dos fechas
fun Habito.proporcion(hechos: Set<LocalDate>, desde: LocalDate, hasta: LocalDate): Proporcion {
    val inicio = if (desde.isAfter(fechaInicio)) desde else fechaInicio
    if (inicio.isAfter(hasta)) return Proporcion.VACIA

    if (tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) {
        var semana = inicioDeSemana(inicio)
        var h = 0
        var e = 0
        while (!semana.isAfter(hasta)) {
            h += minOf(vecesEnSemana(semana, hechos), vecesPorSemana)
            e += vecesPorSemana
            semana = semana.plusWeeks(1)
        }
        return Proporcion(h, e)
    }

    var h = 0
    var e = 0
    var dia = inicio
    while (!dia.isAfter(hasta)) {
        if (tocaEl(dia)) {
            e++
            if (dia in hechos) h++
        }
        dia = dia.plusDays(1)
    }
    return Proporcion(h, e)
}

// Semana en curso: el día de hoy solo suma si ya está hecho
private fun Habito.proporcionEnCurso(hechos: Set<LocalDate>, lunes: LocalDate, hoy: LocalDate): Proporcion {
    if (tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) return proporcion(hechos, lunes, hoy)
    val hastaAyer = proporcion(hechos, lunes, hoy.minusDays(1))
    val deHoy = if (tocaEl(hoy) && hoy in hechos) Proporcion(1, 1) else Proporcion.VACIA
    return hastaAyer + deHoy
}

// Porcentaje de cada una de las últimas N semanas (la última es la actual).
// null = el hábito todavía no existía esa semana
fun Habito.evolucionSemanal(
    hechos: Set<LocalDate>,
    hoy: LocalDate,
    semanas: Int = 8
): List<Pair<LocalDate, Float?>> {
    val lunesActual = inicioDeSemana(hoy)
    return (semanas - 1 downTo 0).map { i ->
        val lunes = lunesActual.minusWeeks(i.toLong())
        val valor = if (i == 0) proporcionEnCurso(hechos, lunes, hoy)
        else proporcion(hechos, lunes, lunes.plusDays(6))
        lunes to valor.fraccion
    }
}

// La racha más larga desde que empezó el hábito
fun Habito.mejorRacha(hoy: LocalDate, hechos: Set<LocalDate>): Int {
    var mejor = 0
    var actual = 0

    if (tipoFrecuencia == TipoFrecuencia.VECES_POR_SEMANA) {
        val semanaActual = inicioDeSemana(hoy)
        var semana = inicioDeSemana(fechaInicio)
        while (!semana.isAfter(semanaActual)) {
            if (vecesEnSemana(semana, hechos) >= vecesPorSemana) {
                actual++
                mejor = maxOf(mejor, actual)
            } else if (semana != semanaActual) {
                actual = 0
            }
            semana = semana.plusWeeks(1)
        }
        return mejor
    }

    var dia = fechaInicio
    while (!dia.isAfter(hoy)) {
        if (tocaEl(dia)) {
            if (dia in hechos) {
                actual++
                mejor = maxOf(mejor, actual)
            } else if (dia != hoy) {
                actual = 0
            }
        }
        dia = dia.plusDays(1)
    }
    return mejor
}