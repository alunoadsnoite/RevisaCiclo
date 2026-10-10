package br.com.revisaciclo.domain.repository

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import kotlinx.coroutines.flow.Flow

interface RevisaoRepository {
    fun observarDisciplinas(): Flow<List<Disciplina>>
    fun observarAssuntos(): Flow<List<Assunto>>
    fun observarRevisoes(): Flow<List<Revisao>>
    fun observarProgressoPorDisciplina(): Flow<List<ProgressoDisciplina>>
    suspend fun garantirDadosDeDemonstracao()
}
