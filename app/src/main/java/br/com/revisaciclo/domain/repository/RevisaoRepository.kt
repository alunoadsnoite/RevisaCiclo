package br.com.revisaciclo.domain.repository

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RevisaoRepository {
    fun observarDisciplinas(): Flow<List<Disciplina>>
    fun observarDisciplina(id: Long): Flow<Disciplina?>
    fun observarAssuntos(): Flow<List<Assunto>>
    fun observarAssuntosPorDisciplina(disciplinaId: Long): Flow<List<Assunto>>
    fun observarAssunto(id: Long): Flow<Assunto?>
    fun observarRevisoes(): Flow<List<Revisao>>
    fun observarRevisoesPendentes(): Flow<List<RevisaoPendente>>
    fun observarProgressoPorDisciplina(): Flow<List<ProgressoDisciplina>>

    suspend fun criarDisciplina(projetoId: Long, nome: String, cor: Long): Long
    suspend fun atualizarDisciplina(id: Long, nome: String, cor: Long)
    suspend fun excluirDisciplina(id: Long)

    suspend fun criarAssunto(
        disciplinaId: Long,
        titulo: String,
        descricao: String?,
        dataEstudo: LocalDate,
    ): Long

    suspend fun atualizarAssunto(
        id: Long,
        disciplinaId: Long,
        titulo: String,
        descricao: String?,
        dataEstudo: LocalDate,
    )

    suspend fun excluirAssunto(id: Long)

    suspend fun concluirRevisao(revisaoId: Long, avaliacao: AvaliacaoRevisao)
}
