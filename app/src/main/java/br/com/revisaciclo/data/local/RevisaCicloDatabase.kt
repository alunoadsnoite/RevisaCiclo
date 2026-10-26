package br.com.revisaciclo.data.local

import android.content.Context
import br.com.revisaciclo.R
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.revisaciclo.data.local.dao.AssuntoDao
import br.com.revisaciclo.data.local.dao.DisciplinaDao
import br.com.revisaciclo.data.local.dao.ProjetoEstudoDao
import br.com.revisaciclo.data.local.dao.RevisaoDao
import br.com.revisaciclo.data.local.entity.AssuntoEntity
import br.com.revisaciclo.data.local.entity.DisciplinaEntity
import br.com.revisaciclo.data.local.entity.ProjetoEstudoEntity
import br.com.revisaciclo.data.local.entity.RevisaoEntity

@Database(
    entities = [
        ProjetoEstudoEntity::class,
        DisciplinaEntity::class,
        AssuntoEntity::class,
        RevisaoEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RevisaCicloDatabase : RoomDatabase() {
    abstract fun projetoEstudoDao(): ProjetoEstudoDao
    abstract fun disciplinaDao(): DisciplinaDao
    abstract fun assuntoDao(): AssuntoDao
    abstract fun revisaoDao(): RevisaoDao

    companion object {
        private const val NOME_BANCO = "revisaciclo.db"
        private const val MS_POR_DIA = 86_400_000L

        const val NOME_PROJETO_PADRAO = "Meus Estudos"
        const val ID_PROJETO_PADRAO = 1L
        const val COR_PROJETO_PADRAO = 0xFF1565C0L

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

        /**
         * Adiciona os projetos de estudo e vincula disciplinas existentes ao projeto padrão.
         *
         * As chaves estrangeiras só são ligadas no `onOpen` do Room, portanto a troca de
         * tabela abaixo não dispara exclusões em cascata e preserva todos os dados.
         */
        val MIGRACAO_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `projetos_estudo` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `descricao` TEXT,
                        `cor` INTEGER NOT NULL,
                        `criadoEm` INTEGER NOT NULL,
                        `arquivado` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `projetos_estudo` (`id`, `nome`, `descricao`, `cor`, `criadoEm`, `arquivado`)
                    VALUES ($ID_PROJETO_PADRAO, '$NOME_PROJETO_PADRAO', NULL, $COR_PROJETO_PADRAO, ${System.currentTimeMillis()}, 0)
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `disciplinas_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projetoId` INTEGER NOT NULL,
                        `nome` TEXT NOT NULL,
                        `cor` INTEGER NOT NULL,
                        `criadaEm` INTEGER NOT NULL,
                        FOREIGN KEY(`projetoId`) REFERENCES `projetos_estudo`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `disciplinas_new` (`id`, `projetoId`, `nome`, `cor`, `criadaEm`)
                    SELECT `id`, $ID_PROJETO_PADRAO, `nome`, `cor`, `criadaEm` FROM `disciplinas`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `disciplinas`")
                db.execSQL("ALTER TABLE `disciplinas_new` RENAME TO `disciplinas`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_disciplinas_projetoId` ON `disciplinas` (`projetoId`)")
            }
        }

        private fun callbackCriarProjetoPadrao(context: Context) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `projetos_estudo` (`id`, `nome`, `descricao`, `cor`, `criadoEm`, `arquivado`)
                    VALUES ($ID_PROJETO_PADRAO, ?, NULL, $COR_PROJETO_PADRAO, ${System.currentTimeMillis()}, 0)
                    """.trimIndent(),
                    arrayOf(context.getString(R.string.default_project_name)),
                )
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
                    .addMigrations(MIGRACAO_1_2, MIGRACAO_2_3)
                    .addCallback(callbackCriarProjetoPadrao(context.applicationContext))
                    .build()
                    .also { instancia = it }
            }
    }
}
