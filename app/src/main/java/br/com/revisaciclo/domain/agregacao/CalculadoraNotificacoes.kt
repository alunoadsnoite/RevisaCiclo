package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate

/**
 * Contagem global de revisões devidas, usada pelas notificações.
 *
 * Importante: considera todos os projetos **não arquivados**, independentemente do
 * projeto atualmente selecionado. Uma revisão é devida quando não está concluída e
 * sua data agendada é igual ou anterior a [hoje].
 */
object CalculadoraNotificacoes {

    fun contarDevidas(
        revisoes: List<Revisao>,
        assuntos: List<Assunto>,
        disciplinas: List<Disciplina>,
        projetos: List<ProjetoEstudo>,
        hoje: LocalDate,
    ): Int {
        val projetosAtivos = projetos.filter { !it.arquivado }.map { it.id }.toSet()
        val disciplinasAtivas = disciplinas
            .filter { it.projetoId in projetosAtivos }
            .map { it.id }
            .toSet()
        val assuntosAtivos = assuntos
            .filter { it.disciplinaId in disciplinasAtivas }
            .map { it.id }
            .toSet()

        return revisoes.count { revisao ->
            revisao.status != StatusRevisao.CONCLUIDA &&
                revisao.assuntoId in assuntosAtivos &&
                !revisao.agendadaPara.isAfter(hoje)
        }
    }
}
