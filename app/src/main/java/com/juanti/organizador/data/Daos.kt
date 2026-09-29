package com.juanti.organizador.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Una deadline junto con todas las tareas de su plan de acción
data class DeadlineConPlan(
    @Embedded val deadline: Deadline,
    @Relation(parentColumn = "id", entityColumn = "deadlineId")
    val tareas: List<TareaPlan>
)

@Dao
interface DeadlineDao {

    @Query("SELECT * FROM deadlines ORDER BY fecha ASC")
    fun todas(): Flow<List<Deadline>>

    @Transaction
    @Query("SELECT * FROM deadlines WHERE completada = 0 AND fecha >= :desde ORDER BY fecha ASC")
    fun pendientesConPlan(desde: LocalDate): Flow<List<DeadlineConPlan>>

    @Transaction
    @Query("SELECT * FROM deadlines WHERE id = :id")
    fun conPlan(id: Long): Flow<DeadlineConPlan?>

    // Lectura puntual
    @Query("SELECT * FROM deadlines WHERE completada = 0 AND fecha >= :desde ORDER BY fecha ASC LIMIT :limite")
    suspend fun proximas(desde: LocalDate, limite: Int): List<Deadline>

    @Insert
    suspend fun insertar(deadline: Deadline): Long

    @Update
    suspend fun actualizar(deadline: Deadline)

    @Delete
    suspend fun borrar(deadline: Deadline)
}

@Dao
interface TareaPlanDao {

    // Las consultas por fecha ignoran las tareas "sin fecha"

    @Query("SELECT * FROM tareas_plan WHERE sinFecha = 0 AND fecha = :fecha ORDER BY completada ASC, id ASC")
    fun delDia(fecha: LocalDate): Flow<List<TareaPlan>>

    @Query("SELECT * FROM tareas_plan WHERE sinFecha = 0 AND fecha BETWEEN :desde AND :hasta ORDER BY fecha ASC, id ASC")
    fun entre(desde: LocalDate, hasta: LocalDate): Flow<List<TareaPlan>>

    @Query("SELECT * FROM tareas_plan WHERE sinFecha = 0 AND completada = 0 AND fecha < :hoy ORDER BY fecha ASC")
    fun atrasadas(hoy: LocalDate): Flow<List<TareaPlan>>

    // Tareas sin fecha pendientes ("cuando pueda")
    @Query("SELECT * FROM tareas_plan WHERE sinFecha = 1 AND completada = 0 ORDER BY id ASC")
    fun sinFecha(): Flow<List<TareaPlan>>

    // Lectura puntual
    @Query("SELECT * FROM tareas_plan WHERE sinFecha = 0 AND fecha = :fecha ORDER BY completada ASC, id ASC")
    suspend fun delDiaLista(fecha: LocalDate): List<TareaPlan>

    @Query("UPDATE tareas_plan SET completada = :completada WHERE id = :id")
    suspend fun marcar(id: Long, completada: Boolean)

    @Insert
    suspend fun insertar(tarea: TareaPlan): Long

    @Update
    suspend fun actualizar(tarea: TareaPlan)

    @Delete
    suspend fun borrar(tarea: TareaPlan)
}

@Dao
interface HabitoDao {

    @Query("SELECT * FROM habitos WHERE archivado = 0 ORDER BY nombre ASC")
    fun activos(): Flow<List<Habito>>

    @Query("SELECT * FROM registros_habito WHERE fecha BETWEEN :desde AND :hasta")
    fun registrosEntre(desde: LocalDate, hasta: LocalDate): Flow<List<RegistroHabito>>

    // Lecturas puntuales (para widgets)
    @Query("SELECT * FROM habitos WHERE archivado = 0 ORDER BY nombre ASC")
    suspend fun activosLista(): List<Habito>

    @Query("SELECT * FROM registros_habito WHERE fecha BETWEEN :desde AND :hasta")
    suspend fun registrosEntreLista(desde: LocalDate, hasta: LocalDate): List<RegistroHabito>

    // Marcar / desmarcar un hábito como cumplido en un día
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registrar(registro: RegistroHabito)

    @Delete
    suspend fun desregistrar(registro: RegistroHabito)

    @Insert
    suspend fun insertar(habito: Habito): Long

    @Update
    suspend fun actualizar(habito: Habito)

    @Delete
    suspend fun borrar(habito: Habito)
}