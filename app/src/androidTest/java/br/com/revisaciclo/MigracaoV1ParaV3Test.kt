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
 * Verifica a migração completa v1→v2→v3 preservando dados e convertendo datas
 * para *epoch day*.
 *
 * Requer dispositivo/emulador; compila localmente mas só executa via
 * `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class MigracaoV1ParaV3Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RevisaCicloDatabase::class.java,
    )

    @Test
    fun migracaoCompletaPreservaDadosEConverteDatas() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO `disciplinas` (`id`, `nome`, `cor`, `criadaEm`) " +
                    "VALUES (10, 'História', 123, 8640000000)",
            )
            execSQL(
                "INSERT INTO `assuntos` " +
                    "(`id`, `disciplinaId`, `nome`, `dificuldade`, `criadoEm`) " +
                    "VALUES (20, 10, 'Revolução', 3, 8640000000)",
            )
            execSQL(
                "INSERT INTO `revisoes` " +
                    "(`id`, `assuntoId`, `agendadaPara`, `concluidaEm`, `status`, `intervaloDias`, `indiceCiclo`, `qualidade`) " +
                    "VALUES (30, 20, 17280000000, NULL, 'PENDENTE', 3, 1, 2)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            RevisaCicloDatabase.MIGRACAO_1_2,
            RevisaCicloDatabase.MIGRACAO_2_3,
        )

        assertEquals(1, db.contar("SELECT COUNT(*) FROM `projetos_estudo`"))
        assertEquals("História", db.texto("SELECT `nome` FROM `disciplinas` WHERE `id` = 10"))
        assertEquals(1L, db.long("SELECT `projetoId` FROM `disciplinas` WHERE `id` = 10"))
        assertEquals("Revolução", db.texto("SELECT `nome` FROM `assuntos` WHERE `id` = 20"))
        assertEquals(100L, db.long("SELECT `dataEstudo` FROM `assuntos` WHERE `id` = 20"))
        assertEquals(200L, db.long("SELECT `agendadaPara` FROM `revisoes` WHERE `id` = 30"))

        db.close()
    }

    private companion object {
        const val TEST_DB = "migracao-v1-v3.db"
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
