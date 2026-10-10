package br.com.revisaciclo.domain.backup

import java.time.Clock
import java.time.Instant

/**
 * Gera o backup completo (`.revisaciclo`) e a exportação portátil (JSON).
 *
 * Lê um snapshot consistente e o serializa; nenhuma escrita de dados do usuário
 * é feita aqui. A escrita do arquivo escolhido pelo usuário é feita por
 * [salvarBackup]/[salvarJson] através de [FonteArquivo].
 */
class ServicoBackup(
    private val armazenamento: ArmazenamentoBackup,
    private val fonteArquivo: FonteArquivo,
    private val versaoApp: String,
    private val clock: Clock,
) {

    suspend fun criarBackupBytes(): ByteArray {
        val dados = armazenamento.capturar()
        return SerializadorBackup.serializarBytes(dados, versaoApp, Instant.now(clock))
    }

    suspend fun exportarJsonBytes(): ByteArray {
        val dados = armazenamento.capturar()
        return ExportadorJson.exportarBytes(dados, versaoApp, Instant.now(clock))
    }

    suspend fun salvarBackup(identificador: String) {
        fonteArquivo.escrever(identificador, criarBackupBytes())
    }

    suspend fun salvarJson(identificador: String) {
        fonteArquivo.escrever(identificador, exportarJsonBytes())
    }
}
