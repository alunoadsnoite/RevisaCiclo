package br.com.revisaciclo.domain.notificacao

import br.com.revisaciclo.domain.model.Preferencias
import java.time.ZonedDateTime

/**
 * Decide se uma notificação de revisões deve ser disparada.
 *
 * Regras:
 * - notificações precisam estar ativas;
 * - deve existir ao menos uma revisão devida;
 * - não pode ter notificado no mesmo dia (evita duplicidade);
 * - só notifica a partir do horário preferido (o WorkManager pode executar um
 *   pouco antes; nesse caso aguardamos a próxima janela).
 */
object DecisorNotificacao {

    fun deveNotificar(
        preferencias: Preferencias,
        revisoesDevidas: Int,
        jaNotificadoNoDia: Boolean,
        agora: ZonedDateTime,
    ): Boolean {
        if (!preferencias.notificacoesAtivas) return false
        if (revisoesDevidas <= 0) return false
        if (jaNotificadoNoDia) return false
        return !agora.toLocalTime().isBefore(preferencias.horarioNotificacao)
    }
}
