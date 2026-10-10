package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes
import java.io.IOException
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException

/** Classificação de uma falha de restauração, para mensagens ao usuário. */
enum class TipoFalhaRestauracao {
    LEITURA,
    ARQUIVO_INVALIDO,
    DADOS_INVALIDOS,
    ESCRITA,
}

/** Resultado da análise de um arquivo escolhido pelo usuário. */
sealed interface ResultadoLeituraBackup {
    data class Pronto(
        val documento: BackupDocumento,
        val resumo: ResumoBackup,
    ) : ResultadoLeituraBackup

    data class Invalido(
        val tipo: TipoFalhaRestauracao,
        val mensagem: String,
    ) : ResultadoLeituraBackup
}

/** Resultado de uma restauração confirmada. */
sealed interface ResultadoRestauracao {
    data class Sucesso(val resumo: ResumoBackup) : ResultadoRestauracao

    data class Falha(
        val tipo: TipoFalhaRestauracao,
        val mensagem: String,
        val copiaSegurancaMantida: Boolean,
    ) : ResultadoRestauracao
}

/**
 * Orquestra a restauração segura de um backup.
 *
 * Protocolo:
 * 1. lê e analisa o arquivo (sem tocar nos dados);
 * 2. o usuário confirma (fora deste serviço);
 * 3. grava um snapshot de segurança do estado atual;
 * 4. substitui o Room em uma transação atômica (rollback automático em falha);
 * 5. grava o marcador de operação e aplica as preferências; em interrupção, um
 *    reconcile na próxima abertura conclui as preferências pendentes;
 * 6. limpa o marcador e o snapshot, e reagenda as notificações.
 *
 * Cancelamento é propagado ([CancellationException]); se ocorrer após a
 * substituição do Room, o marcador permanece e a recuperação é acionada na
 * próxima inicialização.
 */
class ServicoRestauracao(
    private val armazenamento: ArmazenamentoBackup,
    private val estadoOperacao: RepositorioEstadoOperacao,
    private val fonteArquivo: FonteArquivo,
    private val armazenamentoSeguranca: ArmazenamentoSeguranca,
    private val gerenciadorNotificacoes: GerenciadorNotificacoes,
    private val versaoApp: String,
    private val clock: Clock,
) {

    suspend fun lerArquivo(identificador: String): ResultadoLeituraBackup {
        val bytes = try {
            fonteArquivo.ler(identificador, BackupFormato.LIMITE_BYTES)
        } catch (e: ArquivoGrandeDemaisException) {
            return ResultadoLeituraBackup.Invalido(
                TipoFalhaRestauracao.ARQUIVO_INVALIDO,
                "O arquivo é maior que o limite permitido.",
            )
        } catch (e: IOException) {
            return ResultadoLeituraBackup.Invalido(
                TipoFalhaRestauracao.LEITURA,
                "Não foi possível ler o arquivo selecionado.",
            )
        } catch (e: SecurityException) {
            return ResultadoLeituraBackup.Invalido(
                TipoFalhaRestauracao.LEITURA,
                "Sem permissão para acessar o arquivo selecionado.",
            )
        }

        val documento = try {
            SerializadorBackup.desserializar(bytes)
        } catch (e: BackupInvalidoException) {
            return ResultadoLeituraBackup.Invalido(
                TipoFalhaRestauracao.ARQUIVO_INVALIDO,
                mensagemBackupInvalido(e.motivo),
            )
        }

        return when (val validacao = ValidadorBackup.validar(documento)) {
            is ResultadoValidacaoBackup.Valido ->
                ResultadoLeituraBackup.Pronto(validacao.documento, validacao.resumo)

            is ResultadoValidacaoBackup.Invalido ->
                ResultadoLeituraBackup.Invalido(
                    TipoFalhaRestauracao.DADOS_INVALIDOS,
                    "O backup não pôde ser validado: ${validacao.mensagem}",
                )
        }
    }

    suspend fun restaurar(documento: BackupDocumento): ResultadoRestauracao {
        val snapshotId = salvarSnapshotSeguranca()

        try {
            armazenamento.substituirRoom(documento.dados)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            apagarSnapshot(snapshotId)
            return ResultadoRestauracao.Falha(
                TipoFalhaRestauracao.ESCRITA,
                "Não foi possível gravar os dados restaurados. Nada foi alterado.",
                copiaSegurancaMantida = false,
            )
        }

        val estado = EstadoOperacao(documento.dados.preferencias)
        try {
            estadoOperacao.gravar(estado)
            armazenamento.aplicarPreferencias(documento.dados.preferencias)
            estadoOperacao.limpar()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ResultadoRestauracao.Falha(
                TipoFalhaRestauracao.ESCRITA,
                "Os dados foram restaurados, mas as preferências serão concluídas quando você reabrir o aplicativo.",
                copiaSegurancaMantida = snapshotId != null,
            )
        }

        apagarSnapshot(snapshotId)
        runCatching { gerenciadorNotificacoes.sincronizar(documento.dados.preferencias) }

        return ResultadoRestauracao.Sucesso(SerializadorBackup.resumo(documento))
    }

    private suspend fun salvarSnapshotSeguranca(): String? {
        val atual = try {
            armazenamento.capturar()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        return try {
            val bytes = SerializadorBackup.serializarBytes(atual, versaoApp, Instant.now(clock))
            armazenamentoSeguranca.salvar(bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun apagarSnapshot(identificador: String?) {
        if (identificador == null) return
        runCatching { armazenamentoSeguranca.apagar(identificador) }
    }

    private fun mensagemBackupInvalido(motivo: MotivoBackupInvalido): String = when (motivo) {
        MotivoBackupInvalido.VAZIO -> "O arquivo está vazio."
        MotivoBackupInvalido.GRANDE_DEMAIS -> "O arquivo é maior que o limite permitido."
        MotivoBackupInvalido.JSON_INVALIDO -> "O arquivo não é um JSON válido."
        MotivoBackupInvalido.FORMATO_DESCONHECIDO -> "O arquivo não é um backup do RevisaCiclo."
        MotivoBackupInvalido.VERSAO_FUTURA ->
            "O backup foi criado por uma versão mais nova do aplicativo."
        MotivoBackupInvalido.CHECKSUM_INCORRETO ->
            "O backup parece corrompido (verificação de integridade falhou)."
        MotivoBackupInvalido.CAMPOS_AUSENTES -> "O backup está incompleto."
        MotivoBackupInvalido.VALOR_INVALIDO -> "O backup contém valores inválidos."
    }
}
