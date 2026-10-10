package br.com.revisaciclo.domain.model

import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.time.LocalTime

enum class TemaApp {
    CLARO,
    ESCURO,
    AUTOMATICO,
}

/**
 * Preferências do usuário persistidas via DataStore.
 *
 * [intervalosDias] representa os estágios do ciclo de revisão espaçada. O padrão
 * permanece [MotorRevisaoEspacada.INTERVALOS_PADRAO]; valores personalizados só se
 * aplicam a novos agendamentos, nunca de forma retroativa.
 */
data class Preferencias(
    val notificacoesAtivas: Boolean = true,
    val horarioNotificacao: LocalTime = LocalTime.of(8, 0),
    val metaDiaria: Int = META_DIARIA_PADRAO,
    val tema: TemaApp = TemaApp.AUTOMATICO,
    val intervalosDias: List<Int> = MotorRevisaoEspacada.INTERVALOS_PADRAO,
    val projetoAtivoId: Long? = null,
) {
    companion object {
        const val META_DIARIA_PADRAO = 20
        const val META_DIARIA_MINIMA = 1
        const val META_DIARIA_MAXIMA = 200
    }
}
