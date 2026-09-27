package com.juanti.organizador.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Deadline::class, TareaPlan::class, Habito::class, RegistroHabito::class],
    version = 1,
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

        // Devuelve siempre la misma base de datos, la pida la app o un widget
        fun obtener(context: Context): BaseDeDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDeDatos::class.java,
                    "organizador.db"
                ).build().also { instancia = it }
            }
    }
}