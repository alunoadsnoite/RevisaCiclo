package br.com.revisaciclo.data.backup

import br.com.revisaciclo.domain.backup.ArmazenamentoSeguranca
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Guarda o snapshot de segurança em armazenamento privado do aplicativo.
 *
 * O identificador devolvido é apenas o nome do arquivo, sem caminho absoluto,
 * para não vazar informações de diretórios em mensagens de erro.
 */
class ArmazenamentoSegurancaArquivos(
    private val diretorio: File,
) : ArmazenamentoSeguranca {

    override suspend fun salvar(conteudo: ByteArray): String = withContext(Dispatchers.IO) {
        diretorio.mkdirs()
        val nome = "seguranca-${System.currentTimeMillis()}.revisaciclo"
        File(diretorio, nome).writeBytes(conteudo)
        nome
    }

    override suspend fun apagar(identificador: String) = withContext(Dispatchers.IO) {
        runCatching { File(diretorio, identificador).delete() }
        Unit
    }
}
