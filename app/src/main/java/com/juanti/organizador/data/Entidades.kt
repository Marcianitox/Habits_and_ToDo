package com.juanti.organizador.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate

// ---------- DEADLINES ----------

enum class TipoDeadline { EXAMEN, ENTREGA, OTRO }

@Entity(tableName = "deadlines")
data class Deadline(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val tipo: TipoDeadline = TipoDeadline.EXAMEN,
    val materia: String? = null,
    val fecha: LocalDate,
    val notas: String? = null,
    val completada: Boolean = false,
    // Cuántos días antes avisar (0 = el mismo día). Vacío = sin recordatorios.
    @ColumnInfo(defaultValue = "''")
    val recordatorios: Set<Int> = emptySet(),
    // Hora del aviso, en minutos desde las 00:00 (1200 = 20:00)
    @ColumnInfo(defaultValue = "1200")
    val horaRecordatorio: Int = 20 * 60
)

// ---------- PLAN DE ACCIÓN ----------

@Entity(
    tableName = "tareas_plan",
    foreignKeys = [ForeignKey(
        entity = Deadline::class,
        parentColumns = ["id"],
        childColumns = ["deadlineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("deadlineId"), Index("fecha")]
)
data class TareaPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val fecha: LocalDate,
    val deadlineId: Long? = null,
    val completada: Boolean = false
)

// ---------- HÁBITOS ----------

enum class TipoFrecuencia { DIARIA, CADA_N_DIAS, DIAS_SEMANA, VECES_POR_SEMANA }

@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipoFrecuencia: TipoFrecuencia = TipoFrecuencia.DIARIA,
    val cadaNDias: Int = 1,
    val diasSemana: Set<DayOfWeek> = emptySet(),
    val vecesPorSemana: Int = 1,
    val fechaInicio: LocalDate = LocalDate.now(),
    val archivado: Boolean = false
)

@Entity(
    tableName = "registros_habito",
    primaryKeys = ["habitoId", "fecha"],
    foreignKeys = [ForeignKey(
        entity = Habito::class,
        parentColumns = ["id"],
        childColumns = ["habitoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitoId"), Index("fecha")]
)
data class RegistroHabito(
    val habitoId: Long,
    val fecha: LocalDate
)

// ---------- CONVERSORES ----------
// La base de datos no sabe guardar fechas ni conjuntos,
// así que los traducimos a números y texto.

class Convertidores {
    @TypeConverter
    fun fechaANumero(fecha: LocalDate): Long = fecha.toEpochDay()

    @TypeConverter
    fun numeroAFecha(valor: Long): LocalDate = LocalDate.ofEpochDay(valor)

    @TypeConverter
    fun diasATexto(dias: Set<DayOfWeek>): String =
        dias.joinToString(",") { it.value.toString() }

    @TypeConverter
    fun textoADias(texto: String): Set<DayOfWeek> =
        if (texto.isBlank()) emptySet()
        else texto.split(",").map { DayOfWeek.of(it.trim().toInt()) }.toSet()

    @TypeConverter
    fun enterosATexto(valores: Set<Int>): String =
        valores.sorted().joinToString(",")

    @TypeConverter
    fun textoAEnteros(texto: String): Set<Int> =
        if (texto.isBlank()) emptySet()
        else texto.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
}