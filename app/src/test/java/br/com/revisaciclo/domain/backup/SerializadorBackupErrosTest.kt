package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Preferencias
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SerializadorBackupErrosTest {

    private val dadosMin = BackupDados(
        projetos = emptyList(),
        disciplinas = emptyList(),
        assuntos = emptyList(),
        revisoes = emptyList(),
        preferencias = Preferencias(),
    )

    @Test
    fun `lanca quando checksum incorreto`() {
        val agora = Instant.EPOCH
        val texto = SerializadorBackup.serializar(dadosMin, "0.1.0", agora)
        val marcador = "\"valor\": \""
        val start = texto.indexOf(marcador)
        if (start < 0) throw IllegalStateException("valor não encontrado")
        val valStart = start + marcador.length
        val valEnd = texto.indexOf("\"", valStart)
        if (valEnd < 0) throw IllegalStateException("fim não encontrado")
        val hex = texto.substring(valStart, valEnd)
        val primeiro = if (hex.isEmpty() || hex[0] == 'f') '0' else 'f'
        val trocado = primeiro + hex.drop(1)
        val jsonErrado = texto.substring(0, valStart) + trocado + texto.substring(valEnd)
        val ex = assertThrows(BackupInvalidoException::class.java) {
            SerializadorBackup.desserializar(jsonErrado)
        }
        assertEquals(MotivoBackupInvalido.CHECKSUM_INCORRETO, ex.motivo)
    }

    @Test
    fun `lanca quando formato desconhecido`() {
        val checksum = "a".repeat(64)
        val json = """{"formato":"outro","versaoFormato":1,"versaoApp":"0.1.0","versaoSchemaBanco":3,"criadoEm":0,"checksum":{"algoritmo":"SHA-256","valor":"$checksum"},"contagens":{"projetos":0,"disciplinas":0,"assuntos":0,"revisoes":0},"payload":{"projetos":[],"disciplinas":[],"assuntos":[],"revisoes":[],"preferencias":{"notificacoesAtivas":true,"horarioHora":8,"horarioMinuto":0,"metaDiaria":20,"tema":"AUTOMATICO","intervalosDias":[1,3,7,15,30]}}}"""
        val ex = assertThrows(BackupInvalidoException::class.java) {
            SerializadorBackup.desserializar(json)
        }
        assertEquals(MotivoBackupInvalido.FORMATO_DESCONHECIDO, ex.motivo)
    }

    @Test
    fun `lanca quando versao futura`() {
        val checksum = "a".repeat(64)
        val json = """{"formato":"revisaciclo-backup","versaoFormato":999,"versaoApp":"0.1.0","versaoSchemaBanco":3,"criadoEm":0,"checksum":{"algoritmo":"SHA-256","valor":"$checksum"},"contagens":{"projetos":0,"disciplinas":0,"assuntos":0,"revisoes":0},"payload":{"projetos":[],"disciplinas":[],"assuntos":[],"revisoes":[],"preferencias":{"notificacoesAtivas":true,"horarioHora":8,"horarioMinuto":0,"metaDiaria":20,"tema":"AUTOMATICO","intervalosDias":[1,3,7,15,30]}}}"""
        val ex = assertThrows(BackupInvalidoException::class.java) {
            SerializadorBackup.desserializar(json)
        }
        assertEquals(MotivoBackupInvalido.VERSAO_FUTURA, ex.motivo)
    }

    @Test
    fun `lanca quando json invalido`() {
        val ex = assertThrows(BackupInvalidoException::class.java) {
            SerializadorBackup.desserializar("{ nao json }")
        }
        assertEquals(MotivoBackupInvalido.JSON_INVALIDO, ex.motivo)
    }

    @Test
    fun `lanca quando campos ausentes`() {
        val ex = assertThrows(BackupInvalidoException::class.java) {
            SerializadorBackup.desserializar("{}")
        }
        assertEquals(MotivoBackupInvalido.FORMATO_DESCONHECIDO, ex.motivo)
    }
}
