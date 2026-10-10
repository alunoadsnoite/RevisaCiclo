package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.RevisaoPendente

/**
 * Filtragem em memória dos dados pelo projeto ativo.
 *
 * O projeto ativo define um conjunto de disciplinas; assuntos, revisões e progresso
 * descendem delas por relacionamento. Quando [projetoId] é nulo, nada é retornado
 * (não há projeto ativo válido).
 */
object FiltroProjeto {

    fun disciplinas(disciplinas: List<Disciplina>, projetoId: Long?): List<Disciplina> =
        if (projetoId == null) emptyList() else disciplinas.filter { it.projetoId == projetoId }

    fun assuntos(assuntos: List<Assunto>, disciplinaIds: Set<Long>): List<Assunto> =
        assuntos.filter { it.disciplinaId in disciplinaIds }

    fun revisoes(revisoes: List<Revisao>, assuntoIds: Set<Long>): List<Revisao> =
        revisoes.filter { it.assuntoId in assuntoIds }

    fun revisoesPendentes(
        pendentes: List<RevisaoPendente>,
        disciplinaIds: Set<Long>,
    ): List<RevisaoPendente> = pendentes.filter { it.disciplinaId in disciplinaIds }

    fun progresso(
        progressos: List<ProgressoDisciplina>,
        disciplinaIds: Set<Long>,
    ): List<ProgressoDisciplina> = progressos.filter { it.disciplinaId in disciplinaIds }
}
