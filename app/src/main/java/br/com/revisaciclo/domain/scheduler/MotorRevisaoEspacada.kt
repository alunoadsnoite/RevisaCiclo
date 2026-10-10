package br.com.revisaciclo.domain.scheduler

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import java.time.LocalDate

/**
 * Motor de revisão espaçada com ciclos crescentes.
 *
 * O primeiro ciclo acontece 1 dia após o estudo; a cada avaliação positiva o
 * ciclo avança e o intervalo aumenta.
 *
 * ### Política após o último estágio
 * Quando o índice do ciclo ultrapassa a quantidade de intervalos configurados, o
 * intervalo permanece fixado no **último** valor da lista (o ciclo continua a
 * avançar internamente, mas o espaçamento não cresce mais). Isso preserva o
 * comportamento histórico de `[1, 3, 7, 15, 30]`, em que revisões após o 5º ciclo
 * continuam acontecendo a cada 30 dias.
 *
 * As avaliações [AvaliacaoRevisao] continuam com o mesmo significado: ESQUECI
 * reinicia no ciclo 1, DIFICIL repete o ciclo, BOM avança um ciclo e FACIL avança
 * dois.
 *
 * ### Intervalos personalizados
 * Os intervalos podem ser personalizados pelo usuário (positivos e estritamente
 * crescentes). O motor recebe a lista por parâmetro, com padrão
 * [INTERVALOS_PADRAO], sem duplicar lógica. A mudança de intervalos **não** é
 * retroativa: revisões já agendadas no banco mantêm suas datas, pois são
 * recalculadas apenas quando um novo agendamento é criado.
 */
object MotorRevisaoEspacada {

    val INTERVALOS_PADRAO: List<Int> = listOf(1, 3, 7, 15, 30)

    fun intervaloDias(ciclo: Int, intervalos: List<Int> = INTERVALOS_PADRAO): Int {
        require(intervalos.isNotEmpty()) { "É necessário ao menos um intervalo" }
        val indice = (ciclo - 1).coerceIn(0, intervalos.lastIndex)
        return intervalos[indice]
    }

    fun proximoCiclo(cicloAtual: Int, avaliacao: AvaliacaoRevisao): Int = when (avaliacao) {
        AvaliacaoRevisao.ESQUECI -> 1
        AvaliacaoRevisao.DIFICIL -> cicloAtual.coerceAtLeast(1)
        AvaliacaoRevisao.BOM -> cicloAtual + 1
        AvaliacaoRevisao.FACIL -> cicloAtual + 2
    }

    fun agendar(
        dataReferencia: LocalDate,
        ciclo: Int,
        intervalos: List<Int> = INTERVALOS_PADRAO,
    ): Agendamento {
        val intervalo = intervaloDias(ciclo, intervalos)
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
        intervalos: List<Int> = INTERVALOS_PADRAO,
    ): Agendamento = agendar(dataReferencia, proximoCiclo(cicloAtual, avaliacao), intervalos)

    fun agendamentoInicial(
        dataEstudo: LocalDate,
        intervalos: List<Int> = INTERVALOS_PADRAO,
    ): Agendamento = agendar(dataEstudo, 1, intervalos)

    fun estaAtrasada(proximaRevisao: LocalDate?, hoje: LocalDate): Boolean =
        proximaRevisao != null && proximaRevisao.isBefore(hoje)
}
