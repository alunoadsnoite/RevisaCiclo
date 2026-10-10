package br.com.revisaciclo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.revisaciclo.data.local.dao.AssuntoDao
import br.com.revisaciclo.data.local.dao.DisciplinaDao
import br.com.revisaciclo.data.local.dao.RevisaoDao
import br.com.revisaciclo.data.local.entity.AssuntoEntity
import br.com.revisaciclo.data.local.entity.DisciplinaEntity
import br.com.revisaciclo.data.local.entity.RevisaoEntity

@Database(
    entities = [
        DisciplinaEntity::class,
        AssuntoEntity::class,
        RevisaoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RevisaCicloDatabase : RoomDatabase() {
    abstract fun disciplinaDao(): DisciplinaDao
    abstract fun assuntoDao(): AssuntoDao
    abstract fun revisaoDao(): RevisaoDao

    companion object {
        private const val NOME_BANCO = "revisaciclo.db"

        @Volatile
        private var instancia: RevisaCicloDatabase? = null

        fun getInstance(context: Context): RevisaCicloDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    RevisaCicloDatabase::class.java,
                    NOME_BANCO,
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instancia = it }
            }
    }
}
