package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.ProgressoMeta
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate

/**
 * Calcula o progresso da meta diária contando revisões **concluídas hoje**.
 *
 * A comparação usa a data de conclusão (`concluidaEm`) em epoch-day, o que trata
 * corretamente a virada do dia: revisões concluídas ontem deixam de contar hoje.
 */
object CalculadoraMeta {

    fun calcular(
        revisoes: List<Revisao>,
        hoje: LocalDate,
        metaDiaria: Int,
    ): ProgressoMeta {
        val concluidasHoje = revisoes.count { revisao ->
            revisao.status == StatusRevisao.CONCLUIDA && revisao.concluidaEm == hoje
        }
        return ProgressoMeta(concluidasHoje = concluidasHoje, metaDiaria = metaDiaria)
    }
}
