package br.com.revisaciclo.data.backup

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Publica um arquivo gerado pelo aplicativo na área de cache e devolve uma URI
 * de conteúdo compartilhável (via [FileProvider]) para uso com `ACTION_SEND`.
 *
 * O domínio não conhece esta classe: ela é usada apenas pela camada de
 * interface, que nunca expõe caminhos privados ao usuário.
 */
class PublicadorArquivo(private val appContext: Context) {

    @Throws(IOException::class)
    suspend fun publicar(nomeArquivo: String, conteudo: ByteArray): String =
        withContext(Dispatchers.IO) {
            val diretorio = File(appContext.cacheDir, DIRETORIO).apply { mkdirs() }
            removerAntigos(diretorio)
            val arquivo = File(diretorio, nomeArquivo.sanitizar())
            arquivo.writeBytes(conteudo)
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                arquivo,
            ).toString()
        }

    /**
     * Remove arquivos compartilhados anteriormente para liberar espaço. Seguro
     * para chamar na inicialização do processo, quando não há compartilhamento
     * em andamento.
     */
    suspend fun limpar() = withContext(Dispatchers.IO) {
        File(appContext.cacheDir, DIRETORIO).listFiles()?.forEach { it.delete() }
    }

    private fun removerAntigos(diretorio: File) {
        val limite = System.currentTimeMillis() - VALIDADE_ARQUIVOS_MS
        diretorio.listFiles()?.forEach { arquivo ->
            if (arquivo.lastModified() < limite) arquivo.delete()
        }
    }

    private fun String.sanitizar(): String =
        replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "revisaciclo.dat" }

    private companion object {
        const val DIRETORIO = "shared"
        const val VALIDADE_ARQUIVOS_MS = 24L * 60L * 60L * 1000L
    }
}
