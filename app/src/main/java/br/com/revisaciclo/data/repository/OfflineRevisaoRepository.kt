package br.com.revisaciclo.data.repository

import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.local.entity.toDomain
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.repository.RevisaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineRevisaoRepository(
    database: RevisaCicloDatabase,
) : RevisaoRepository {
    private val disciplinaDao = database.disciplinaDao()
    private val assuntoDao = database.assuntoDao()
    private val revisaoDao = database.revisaoDao()

    override fun observarDisciplinas(): Flow<List<Disciplina>> =
        disciplinaDao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarAssuntos(): Flow<List<Assunto>> =
        assuntoDao.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override fun observarRevisoes(): Flow<List<Revisao>> =
        revisaoDao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarProgressoPorDisciplina(): Flow<List<ProgressoDisciplina>> =
        revisaoDao.observarResumoPorDisciplina().map { linhas ->
            linhas.map { linha ->
                ProgressoDisciplina(
                    disciplinaId = linha.disciplinaId,
                    nome = linha.nome,
                    cor = linha.cor,
                    totalRevisoes = linha.totalRevisoes,
                    concluidas = linha.concluidas,
                )
            }
        }

    override suspend fun garantirDadosDeDemonstracao() {
        if (disciplinaDao.contar() > 0) return
        DemoDataSeeder.popular(disciplinaDao, assuntoDao, revisaoDao)
    }
}
