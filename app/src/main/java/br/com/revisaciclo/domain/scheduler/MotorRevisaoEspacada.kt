package br.com.revisaciclo.domain.scheduler

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import java.time.LocalDate

/**
 * Motor de revisão espaçada com ciclos crescentes.
 *
 * O primeiro ciclo acontece 1 dia após o estudo; a cada avaliação positiva o
 * ciclo avança e o intervalo aumenta. Após o último intervalo o assunto segue
 * sendo revisado a cada 30 dias (o ciclo não termina).
 */
object MotorRevisaoEspacada {

    val INTERVALOS_DIAS: List<Int> = listOf(1, 3, 7, 15, 30)

    private val ULTIMO_INDICE = INTERVALOS_DIAS.lastIndex

    fun intervaloDias(ciclo: Int): Int {
        val indice = (ciclo - 1).coerceIn(0, ULTIMO_INDICE)
        return INTERVALOS_DIAS[indice]
    }

    fun proximoCiclo(cicloAtual: Int, avaliacao: AvaliacaoRevisao): Int = when (avaliacao) {
        AvaliacaoRevisao.ESQUECI -> 1
        AvaliacaoRevisao.DIFICIL -> cicloAtual.coerceAtLeast(1)
        AvaliacaoRevisao.BOM -> cicloAtual + 1
        AvaliacaoRevisao.FACIL -> cicloAtual + 2
    }

    fun agendar(dataReferencia: LocalDate, ciclo: Int): Agendamento {
        val intervalo = intervaloDias(ciclo)
        return Agendamento(
            ciclo = ciclo,
            intervaloDias = intervalo,
            proximaRevisao = dataReferencia.plusDays(intervalo.toLong()),
        )
    }

    fun proximoAgendamento(
        dataReferencia: LocalDate,
        cicloAtual: Int,
        avaliacao: AvaliacaoRevisao,
    ): Agendamento = agendar(dataReferencia, proximoCiclo(cicloAtual, avaliacao))

    fun agendamentoInicial(dataEstudo: LocalDate): Agendamento = agendar(dataEstudo, 1)

    fun estaAtrasada(proximaRevisao: LocalDate?, hoje: LocalDate): Boolean =
        proximaRevisao != null && proximaRevisao.isBefore(hoje)
}
