package br.com.revisaciclo.domain.notificacao

import br.com.revisaciclo.domain.model.Preferencias

/**
 * Abstrai o agendamento da verificação de revisões em segundo plano.
 *
 * A implementação usa WorkManager. O método [garantirAgendado] é idempotente e é
 * chamado na inicialização do app; [reagendar] substitui o trabalho existente e é
 * chamado quando preferências relevantes mudam ou após eventos de sistema
 * (reinicialização, mudança de data/fuso).
 */
interface AgendadorNotificacoes {

    fun garantirAgendado(preferencias: Preferencias)

    fun reagendar(preferencias: Preferencias)

    fun cancelar()
}
