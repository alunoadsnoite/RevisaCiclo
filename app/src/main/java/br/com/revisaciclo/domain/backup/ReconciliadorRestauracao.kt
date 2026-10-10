package br.com.revisaciclo.domain.backup

import br.com.revisaciclo.domain.notificacao.GerenciadorNotificacoes

/**
 * Conclui uma restauração que ficou pela metade: se houver um marcador de
 * operação (preferências pendentes), as aplica e reagenda as notificações,
 * limpando o marcador em seguida.
 *
 * Deve ser chamado na inicialização do aplicativo, antes de a interface reagir
 * aos fluxos de dados.
 */
class ReconciliadorRestauracao(
    private val estadoOperacao: RepositorioEstadoOperacao,
    private val armazenamento: ArmazenamentoBackup,
    private val gerenciadorNotificacoes: GerenciadorNotificacoes,
) {

    /** @return `true` se havia uma operação pendente e ela foi concluída. */
    suspend fun reconciliar(): Boolean {
        val estado = estadoOperacao.ler() ?: return false
        val pendentes = estado.preferenciasPendentes
        armazenamento.aplicarPreferencias(pendentes)
        gerenciadorNotificacoes.sincronizar(pendentes)
        estadoOperacao.limpar()
        return true
    }
}
