package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.preferencias.ResultadoIntervalos
import br.com.revisaciclo.domain.preferencias.ValidadorIntervalos

enum class MotivoValidacaoBackup {
    CONTAGENS_INCONSISTENTES,
    LIMITE_EXCEDIDO,
    IDS_DUPLICADOS,
    ID_INVALIDO,
    RELACAO_INVALIDA,
    CAMPO_OBRIGATORIO,
    PREFERENCIAS_INVALIDAS,
}

sealed interface ResultadoValidacaoBackup {
    data class Valido(
        val documento: BackupDocumento,
        val resumo: ResumoBackup,
    ) : ResultadoValidacaoBackup

    data class Invalido(
        val motivo: MotivoValidacaoBackup,
        val mensagem: String,
    ) : ResultadoValidacaoBackup
}

/**
 * Validação semântica do conteúdo restaurado: unicidade de IDs, integridade
 * referencial, limites de campos e coerência das preferências. É executada
 * **antes** de qualquer escrita no banco.
 */
object ValidadorBackup {

    fun validar(documento: BackupDocumento): ResultadoValidacaoBackup {
        val dados = documento.dados

        val colecoes = listOf(
            dados.projetos.size to "projetos",
            dados.disciplinas.size to "disciplinas",
            dados.assuntos.size to "assuntos",
            dados.revisoes.size to "revisões",
        )
        colecoes.firstOrNull { it.first > BackupFormato.LIMITE_REGISTROS }?.let {
            return invalido(
                MotivoValidacaoBackup.LIMITE_EXCEDIDO,
                "Há registros demais em ${it.second}.",
            )
        }

        val contagens = documento.cabecalho.contagens
        if (contagens.projetos != dados.projetos.size ||
            contagens.disciplinas != dados.disciplinas.size ||
            contagens.assuntos != dados.assuntos.size ||
            contagens.revisoes != dados.revisoes.size
        ) {
            return invalido(
                MotivoValidacaoBackup.CONTAGENS_INCONSISTENTES,
                "As contagens do cabeçalho não conferem com o conteúdo.",
            )
        }

        validarIds(dados.projetos.map { it.id }, "projetos")?.let { return it }
        validarIds(dados.disciplinas.map { it.id }, "disciplinas")?.let { return it }
        validarIds(dados.assuntos.map { it.id }, "assuntos")?.let { return it }
        validarIds(dados.revisoes.map { it.id }, "revisões")?.let { return it }

        dados.projetos.firstOrNull { it.nome.isBlank() }?.let {
            return invalido(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, "Projeto sem nome.")
        }
        dados.disciplinas.firstOrNull { it.nome.isBlank() }?.let {
            return invalido(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, "Disciplina sem nome.")
        }
        dados.assuntos.firstOrNull { it.titulo.isBlank() }?.let {
            return invalido(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, "Assunto sem título.")
        }
        dados.assuntos.firstOrNull { it.indiceCiclo < 0 }?.let {
            return invalido(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, "Assunto com ciclo inválido.")
        }
        dados.revisoes.firstOrNull { it.indiceCiclo < 0 || it.intervaloDias <= 0 }?.let {
            return invalido(MotivoValidacaoBackup.CAMPO_OBRIGATORIO, "Revisão com intervalo inválido.")
        }

        val projetoIds = dados.projetos.map { it.id }.toSet()
        val disciplinaIds = dados.disciplinas.map { it.id }.toSet()
        val assuntoIds = dados.assuntos.map { it.id }.toSet()

        dados.disciplinas.firstOrNull { it.projetoId !in projetoIds }?.let {
            return invalido(
                MotivoValidacaoBackup.RELACAO_INVALIDA,
                "Há uma disciplina ligada a um projeto inexistente.",
            )
        }
        dados.assuntos.firstOrNull { it.disciplinaId !in disciplinaIds }?.let {
            return invalido(
                MotivoValidacaoBackup.RELACAO_INVALIDA,
                "Há um assunto ligado a uma disciplina inexistente.",
            )
        }
        dados.revisoes.firstOrNull { it.assuntoId !in assuntoIds }?.let {
            return invalido(
                MotivoValidacaoBackup.RELACAO_INVALIDA,
                "Há uma revisão ligada a um assunto inexistente.",
            )
        }

        validarPreferencias(dados.preferencias, projetoIds)?.let { (motivo, mensagem) ->
            return invalido(motivo, mensagem)
        }

        return ResultadoValidacaoBackup.Valido(
            documento = documento,
            resumo = SerializadorBackup.resumo(documento),
        )
    }

    private fun validarPreferencias(
        preferencias: Preferencias,
        projetoIds: Set<Long>,
    ): Pair<MotivoValidacaoBackup, String>? {
        if (preferencias.metaDiaria < Preferencias.META_DIARIA_MINIMA ||
            preferencias.metaDiaria > Preferencias.META_DIARIA_MAXIMA
        ) {
            return MotivoValidacaoBackup.PREFERENCIAS_INVALIDAS to
                "Meta diária fora do intervalo permitido."
        }
        if (ValidadorIntervalos.validar(preferencias.intervalosDias) !is ResultadoIntervalos.Valido) {
            return MotivoValidacaoBackup.PREFERENCIAS_INVALIDAS to
                "Intervalos de revisão inválidos."
        }
        val projetoAtivo = preferencias.projetoAtivoId
        if (projetoAtivo != null && projetoAtivo !in projetoIds) {
            return MotivoValidacaoBackup.RELACAO_INVALIDA to
                "O projeto ativo não existe no backup."
        }
        val hora = preferencias.horarioNotificacao.hour
        val minuto = preferencias.horarioNotificacao.minute
        if (hora !in 0..23 || minuto !in 0..59) {
            return MotivoValidacaoBackup.PREFERENCIAS_INVALIDAS to
                "Horário de notificação inválido."
        }
        return null
    }

    private fun validarIds(ids: List<Long>, nome: String): ResultadoValidacaoBackup.Invalido? {
        if (ids.any { it <= 0 }) {
            return invalido(MotivoValidacaoBackup.ID_INVALIDO, "Há um ID inválido em $nome.")
        }
        if (ids.toSet().size != ids.size) {
            return invalido(
                MotivoValidacaoBackup.IDS_DUPLICADOS,
                "Há IDs duplicados em $nome.",
            )
        }
        return null
    }

    private fun invalido(motivo: MotivoValidacaoBackup, mensagem: String) =
        ResultadoValidacaoBackup.Invalido(motivo, mensagem)
}
