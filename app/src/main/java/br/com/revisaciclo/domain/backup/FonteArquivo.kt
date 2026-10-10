package br.com.revisaciclo.domain.backup

import java.io.IOException

/** Erro de leitura quando o arquivo ultrapassa o limite permitido. */
class ArquivoGrandeDemaisException : IOException("Arquivo maior que o limite permitido.")

/**
 * Acesso a arquivos escolhidos pelo usuário via Storage Access Framework.
 *
 * O [identificador] é opaco para o domínio (a implementação usa a URI do SAF).
 */
interface FonteArquivo {
    @Throws(IOException::class)
    suspend fun ler(identificador: String, limiteBytes: Long): ByteArray

    @Throws(IOException::class)
    suspend fun escrever(identificador: String, conteudo: ByteArray)
}

/**
 * Guarda um snapshot de segurança (backup do estado atual antes de restaurar)
 * em armazenamento privado do aplicativo.
 */
interface ArmazenamentoSeguranca {
    /** Salva o conteúdo e devolve um identificador curto (sem caminhos privados). */
    suspend fun salvar(conteudo: ByteArray): String

    suspend fun apagar(identificador: String)
}
