package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AssuntoRoda
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstagioRoda
import br.com.revisaciclo.domain.model.RodaRevisao
import br.com.revisaciclo.domain.model.SituacaoRevisao
import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.time.LocalDate

object CalculadoraRoda {

    private const val COR_DISCIPLINA_AUSENTE = 0xFF9E9E9EL

    fun montar(
        assuntos: List<Assunto>,
        disciplinas: Map<Long, Disciplina>,
        hoje: LocalDate,
        intervalos: List<Int> = MotorRevisaoEspacada.INTERVALOS_DIAS,
    ): RodaRevisao {
        val porEstagio = intervalos.associateWith { mutableListOf<AssuntoRoda>() }

        assuntos.forEach { assunto ->
            val intervalo = MotorRevisaoEspacada.intervaloDias(assunto.indiceCiclo)
            porEstagio[intervalo]?.add(
                AssuntoRoda(
                    id = assunto.id,
                    titulo = assunto.titulo,
                    disciplinaNome = disciplinas[assunto.disciplinaId]?.nome ?: "Sem disciplina",
                    disciplinaCor = disciplinas[assunto.disciplinaId]?.cor ?: COR_DISCIPLINA_AUSENTE,
                    intervaloDias = intervalo,
                    proximaRevisao = assunto.proximaRevisao,
                    situacao = situacaoDe(assunto, hoje),
                ),
            )
        }

        val estagios = intervalos.map { intervalo ->
            EstagioRoda(
                intervaloDias = intervalo,
                assuntos = porEstagio.getValue(intervalo)
                    .sortedWith(
                        compareBy<AssuntoRoda> { it.situacao.ordinal }
                            .thenBy { it.titulo.lowercase() },
                    ),
            )
        }

        return RodaRevisao(estagios = estagios, totalAssuntos = assuntos.size)
    }

    private fun situacaoDe(assunto: Assunto, hoje: LocalDate): SituacaoRevisao {
        val proxima = assunto.proximaRevisao ?: return SituacaoRevisao.FUTURO
        return when {
            proxima.isBefore(hoje) -> SituacaoRevisao.ATRASADO
            proxima.isEqual(hoje) -> SituacaoRevisao.HOJE
            else -> SituacaoRevisao.FUTURO
        }
    }
}