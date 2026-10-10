package br.com.revisaciclo.domain.notificacao

import br.com.revisaciclo.domain.model.Preferencias

/**
 * Coordena o agendamento conforme as preferências: quando as notificações estão
 * ativas, reagenda para o próximo horário; quando desativadas, cancela o trabalho
 * pendente. Centraliza a regra para ser testável sem depender do Android.
 */
class GerenciadorNotificacoes(private val agendador: AgendadorNotificacoes) {

    fun sincronizar(preferencias: Preferencias) {
        if (preferencias.notificacoesAtivas) {
            agendador.reagendar(preferencias)
        } else {
            agendador.cancelar()
        }
    }
}
