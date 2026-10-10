package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.ContagemDiaria
import br.com.revisaciclo.domain.model.EstatisticaRevisao
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate

object CalculadoraEstatisticas {

    fun calcular(
        revisoes: List<Revisao>,
        hoje: LocalDate,
        janela7Dias: Int = 7,
        janela30Dias: Int = 30,
    ): EstatisticaRevisao {
        val concluidas = revisoes.count { it.status == StatusRevisao.CONCLUIDA }
        val pendentes = revisoes.count { it.status != StatusRevisao.CONCLUIDA }
        val atrasadas = revisoes.count {
            it.status != StatusRevisao.CONCLUIDA && it.agendadaPara.isBefore(hoje)
        }
        return EstatisticaRevisao(
            totalRevisoes = revisoes.size,
            concluidas = concluidas,
            pendentes = pendentes,
            atrasadas = atrasadas,
            evolucao7Dias = evolucaoDiaria(revisoes, hoje, janela7Dias),
            evolucao30Dias = evolucaoDiaria(revisoes, hoje, janela30Dias),
        )
    }

    fun evolucaoDiaria(
        revisoes: List<Revisao>,
        hoje: LocalDate,
        janela: Int,
    ): List<ContagemDiaria> {
        require(janela > 0) { "Janela deve ser maior que zero" }
        val inicio = hoje.minusDays((janela - 1).toLong())
        val contagemPorDia = HashMap<LocalDate, Int>()

        revisoes.forEach { revisao ->
            val concluidaEm = revisao.concluidaEm ?: return@forEach
            if (!concluidaEm.isBefore(inicio) && !concluidaEm.isAfter(hoje)) {
                contagemPorDia[concluidaEm] = (contagemPorDia[concluidaEm] ?: 0) + 1
            }
        }

        return (0 until janela).map { deslocamento ->
            val data = inicio.plusDays(deslocamento.toLong())
            ContagemDiaria(data = data, quantidade = contagemPorDia[data] ?: 0)
        }
    }
}