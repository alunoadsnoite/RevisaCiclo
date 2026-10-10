package br.com.revisaciclo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true,
)
abstract class RevisaCicloDatabase : RoomDatabase() {
    abstract fun disciplinaDao(): DisciplinaDao
    abstract fun assuntoDao(): AssuntoDao
    abstract fun revisaoDao(): RevisaoDao

    companion object {
        private const val NOME_BANCO = "revisaciclo.db"
        private const val MS_POR_DIA = 86_400_000L

        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `assuntos_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `disciplinaId` INTEGER NOT NULL,
                        `nome` TEXT NOT NULL,
                        `descricao` TEXT,
                        `dataEstudo` INTEGER NOT NULL,
                        `estadoRevisao` TEXT NOT NULL,
                        `indiceCiclo` INTEGER NOT NULL,
                        `proximaRevisao` INTEGER,
                        FOREIGN KEY(`disciplinaId`) REFERENCES `disciplinas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `assuntos_new`
                        (`id`, `disciplinaId`, `nome`, `descricao`, `dataEstudo`, `estadoRevisao`, `indiceCiclo`, `proximaRevisao`)
                    SELECT `id`, `disciplinaId`, `nome`, NULL, `criadoEm` / $MS_POR_DIA, 'NOVO', 0, NULL
                    FROM `assuntos`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `assuntos`")
                db.execSQL("ALTER TABLE `assuntos_new` RENAME TO `assuntos`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_assuntos_disciplinaId` ON `assuntos` (`disciplinaId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `revisoes_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assuntoId` INTEGER NOT NULL,
                        `agendadaPara` INTEGER NOT NULL,
                        `concluidaEm` INTEGER,
                        `status` TEXT NOT NULL,
                        `intervaloDias` INTEGER NOT NULL,
                        `indiceCiclo` INTEGER NOT NULL,
                        `avaliacao` TEXT,
                        FOREIGN KEY(`assuntoId`) REFERENCES `assuntos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `revisoes_new`
                        (`id`, `assuntoId`, `agendadaPara`, `concluidaEm`, `status`, `intervaloDias`, `indiceCiclo`, `avaliacao`)
                    SELECT `id`,
                           `assuntoId`,
                           `agendadaPara` / $MS_POR_DIA,
                           CASE WHEN `concluidaEm` IS NULL THEN NULL ELSE `concluidaEm` / $MS_POR_DIA END,
                           `status`,
                           `intervaloDias`,
                           `indiceCiclo`,
                           NULL
                    FROM `revisoes`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `revisoes`")
                db.execSQL("ALTER TABLE `revisoes_new` RENAME TO `revisoes`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_revisoes_assuntoId` ON `revisoes` (`assuntoId`)")
            }
        }

        @Volatile
        private var instancia: RevisaCicloDatabase? = null

        fun getInstance(context: Context): RevisaCicloDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    RevisaCicloDatabase::class.java,
                    NOME_BANCO,
                )
                    .addMigrations(MIGRACAO_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
