package br.com.revisaciclo.data.backup

import android.content.ContentResolver
import androidx.core.net.toUri
import br.com.revisaciclo.domain.backup.ArquivoGrandeDemaisException
import br.com.revisaciclo.domain.backup.FonteArquivo
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Leitura/escrita de arquivos via Storage Access Framework (ContentResolver).
 *
 * Não usa permissões amplas de armazenamento: o acesso é concedido pelo usuário
 * ao escolher o arquivo (ACTION_OPEN_DOCUMENT / ACTION_CREATE_DOCUMENT).
 */
class FonteArquivoAndroid(
    private val resolver: ContentResolver,
) : FonteArquivo {

    override suspend fun ler(identificador: String, limiteBytes: Long): ByteArray =
        withContext(Dispatchers.IO) {
            val uri = identificador.toUri()
            val entrada = resolver.openInputStream(uri)
                ?: throw FileNotFoundException("Não foi possível abrir o arquivo.")
            entrada.use { stream ->
                val saida = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val lidos = stream.read(buffer)
                    if (lidos < 0) break
                    total += lidos
                    if (total > limiteBytes) {
                        throw ArquivoGrandeDemaisException()
                    }
                    saida.write(buffer, 0, lidos)
                }
                saida.toByteArray()
            }
        }

    override suspend fun escrever(identificador: String, conteudo: ByteArray) =
        withContext(Dispatchers.IO) {
            val uri = identificador.toUri()
            val saida = resolver.openOutputStream(uri, "wt")
                ?: throw IOException("Não foi possível gravar o arquivo.")
            saida.use { stream ->
                stream.write(conteudo)
                stream.flush()
            }
        }
}
