package br.com.revisaciclo

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica a migração v2→v3 (introdução dos projetos de estudo).
 *
 * Requer dispositivo/emulador; compila localmente mas só executa via
 * `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class MigracaoV2ParaV3Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RevisaCicloDatabase::class.java,
    )

    @Test
    fun migracaoPreservaDadosEVinculaProjetoPadrao() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO `disciplinas` (`id`, `nome`, `cor`, `criadaEm`) " +
                    "VALUES (10, 'Matemática', 123, 1000)",
            )
            execSQL(
                "INSERT INTO `assuntos` " +
                    "(`id`, `disciplinaId`, `nome`, `descricao`, `dataEstudo`, `estadoRevisao`, `indiceCiclo`, `proximaRevisao`) " +
                    "VALUES (20, 10, 'Frações', NULL, 20000, 'EM_DIA', 2, 20003)",
            )
            execSQL(
                "INSERT INTO `revisoes` " +
                    "(`id`, `assuntoId`, `agendadaPara`, `concluidaEm`, `status`, `intervaloDias`, `indiceCiclo`, `avaliacao`) " +
                    "VALUES (30, 20, 20001, NULL, 'PENDENTE', 3, 2, NULL)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            RevisaCicloDatabase.MIGRACAO_2_3,
        )

        assertEquals(1, db.contar("SELECT COUNT(*) FROM `projetos_estudo`"))
        assertEquals(
            "Meus Estudos",
            db.texto("SELECT `nome` FROM `projetos_estudo` WHERE `id` = 1"),
        )
        assertEquals(
            1L,
            db.long("SELECT `projetoId` FROM `disciplinas` WHERE `id` = 10"),
        )
        assertEquals("Matemática", db.texto("SELECT `nome` FROM `disciplinas` WHERE `id` = 10"))
        assertEquals(1, db.contar("SELECT COUNT(*) FROM `assuntos` WHERE `id` = 20 AND `disciplinaId` = 10"))
        assertEquals(1, db.contar("SELECT COUNT(*) FROM `revisoes` WHERE `id` = 30 AND `assuntoId` = 20"))
        assertEquals(20001L, db.long("SELECT `agendadaPara` FROM `revisoes` WHERE `id` = 30"))

        db.close()
    }

    private companion object {
        const val TEST_DB = "migracao-v2-v3.db"
    }
}

private fun SupportSQLiteDatabase.contar(sql: String): Int =
    query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

private fun SupportSQLiteDatabase.texto(sql: String): String? =
    query(sql).use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

private fun SupportSQLiteDatabase.long(query: String): Long =
    this.query(query).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }
