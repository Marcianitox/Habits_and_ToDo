package com.juanti.organizador.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Deadline::class, TareaPlan::class, Habito::class, RegistroHabito::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Convertidores::class)
abstract class BaseDeDatos : RoomDatabase() {

    abstract fun deadlineDao(): DeadlineDao
    abstract fun tareaPlanDao(): TareaPlanDao
    abstract fun habitoDao(): HabitoDao

    companion object {
        @Volatile
        private var instancia: BaseDeDatos? = null

        // Versión 1 → 2: recordatorios en las deadlines
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `deadlines` ADD COLUMN `recordatorios` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `deadlines` ADD COLUMN `horaRecordatorio` INTEGER NOT NULL DEFAULT 1200")
            }
        }

        // Versión 2 → 3: tareas sin fecha. Las existentes quedan con fecha.
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tareas_plan` ADD COLUMN `sinFecha` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Versión 3 → 4: descripción en las tareas. Las existentes quedan sin descripción.
        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tareas_plan` ADD COLUMN `descripcion` TEXT")
            }
        }

        // Devuelve siempre la misma base de datos, la pida la app o un widget
        fun obtener(context: Context): BaseDeDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDeDatos::class.java,
                    "organizador.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
                    .build()
                    .also { instancia = it }
            }
    }
}