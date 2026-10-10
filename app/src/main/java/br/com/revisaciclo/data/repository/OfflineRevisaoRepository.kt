package br.com.revisaciclo.data.repository

import androidx.room.withTransaction
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.local.entity.AssuntoEntity
import br.com.revisaciclo.data.local.entity.DisciplinaEntity
import br.com.revisaciclo.data.local.entity.RevisaoEntity
import br.com.revisaciclo.data.local.entity.toDomain
import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.model.StatusRevisao
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.RevisaoRepository
import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OfflineRevisaoRepository(
    private val database: RevisaCicloDatabase,
    private val preferenciasRepository: PreferenciasRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : RevisaoRepository {

    private val disciplinaDao = database.disciplinaDao()
    private val assuntoDao = database.assuntoDao()
    private val revisaoDao = database.revisaoDao()

    private fun hoje(): LocalDate = LocalDate.now(clock)

    private suspend fun intervalos(): List<Int> =
        preferenciasRepository.preferencias.first().intervalosDias

    private fun definirEstado(proximaRevisao: LocalDate): EstadoRevisao =
        if (proximaRevisao.isBefore(hoje())) EstadoRevisao.ATRASADO else EstadoRevisao.EM_DIA

    override fun observarDisciplinas(): Flow<List<Disciplina>> =
        disciplinaDao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarDisciplina(id: Long): Flow<Disciplina?> =
        disciplinaDao.observarPorId(id).map { it?.toDomain() }

    override fun observarAssuntos(): Flow<List<Assunto>> =
        assuntoDao.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override fun observarAssuntosPorDisciplina(disciplinaId: Long): Flow<List<Assunto>> =
        assuntoDao.observarPorDisciplina(disciplinaId).map { lista -> lista.map { it.toDomain() } }

    override fun observarAssunto(id: Long): Flow<Assunto?> =
        assuntoDao.observarPorId(id).map { it?.toDomain() }

    override fun observarRevisoes(): Flow<List<Revisao>> =
        revisaoDao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarRevisoesPendentes(): Flow<List<RevisaoPendente>> =
        revisaoDao.observarPendentes().map { linhas ->
            val hoje = hoje()
            linhas.map { linha ->
                val agendadaPara = LocalDate.ofEpochDay(linha.agendadaPara)
                RevisaoPendente(
                    revisaoId = linha.revisaoId,
                    assuntoId = linha.assuntoId,
                    assuntoTitulo = linha.assuntoTitulo,
                    assuntoDescricao = linha.assuntoDescricao,
                    disciplinaId = linha.disciplinaId,
                    disciplinaNome = linha.disciplinaNome,
                    disciplinaCor = linha.disciplinaCor,
                    agendadaPara = agendadaPara,
                    indiceCiclo = linha.indiceCiclo,
                    intervaloDias = linha.intervaloDias,
                    atrasada = agendadaPara.isBefore(hoje),
                )
            }
        }

    override fun observarProgressoPorDisciplina(): Flow<List<ProgressoDisciplina>> =
        disciplinaDao.observarResumo(hoje().toEpochDay()).map { linhas ->
            linhas.map { linha ->
                ProgressoDisciplina(
                    disciplinaId = linha.disciplinaId,
                    nome = linha.nome,
                    cor = linha.cor,
                    totalAssuntos = linha.totalAssuntos,
                    assuntosAtrasados = linha.assuntosAtrasados,
                )
            }
        }

    override suspend fun criarDisciplina(projetoId: Long, nome: String, cor: Long): Long =
        disciplinaDao.inserir(
            DisciplinaEntity(
                projetoId = projetoId,
                nome = nome,
                cor = cor,
                criadaEm = Instant.now(clock).toEpochMilli(),
            ),
        )

    override suspend fun atualizarDisciplina(id: Long, nome: String, cor: Long) =
        disciplinaDao.atualizar(id = id, nome = nome, cor = cor)

    override suspend fun excluirDisciplina(id: Long) =
        disciplinaDao.excluirPorId(id)

    override suspend fun criarAssunto(
        disciplinaId: Long,
        titulo: String,
        descricao: String?,
        dataEstudo: LocalDate,
    ): Long = database.withTransaction {
        val agendamento = MotorRevisaoEspacada.agendamentoInicial(dataEstudo, intervalos())
        val assuntoId = assuntoDao.inserir(
            AssuntoEntity(
                disciplinaId = disciplinaId,
                titulo = titulo,
                descricao = descricao,
                dataEstudo = dataEstudo.toEpochDay(),
                estadoRevisao = definirEstado(agendamento.proximaRevisao).name,
                indiceCiclo = agendamento.ciclo,
                proximaRevisao = agendamento.proximaRevisao.toEpochDay(),
            ),
        )
        revisaoDao.inserir(
            RevisaoEntity(
                assuntoId = assuntoId,
                agendadaPara = agendamento.proximaRevisao.toEpochDay(),
                status = StatusRevisao.PENDENTE.name,
                intervaloDias = agendamento.intervaloDias,
                indiceCiclo = agendamento.ciclo,
            ),
        )
        assuntoId
    }

    override suspend fun atualizarAssunto(
        id: Long,
        disciplinaId: Long,
        titulo: String,
        descricao: String?,
        dataEstudo: LocalDate,
    ) = database.withTransaction {
        assuntoDao.atualizar(
            id = id,
            disciplinaId = disciplinaId,
            titulo = titulo,
            descricao = descricao,
            dataEstudo = dataEstudo.toEpochDay(),
        )

        val pendente = revisaoDao.buscarPendentePorAssunto(id)
        val ciclo = pendente?.indiceCiclo ?: 1
        val intervalo = pendente?.intervaloDias ?: MotorRevisaoEspacada.intervaloDias(ciclo, intervalos())
        val proximaRevisao = dataEstudo.plusDays(intervalo.toLong())

        if (pendente != null) {
            revisaoDao.atualizarAgendadaPara(pendente.id, proximaRevisao.toEpochDay())
        } else {
            revisaoDao.inserir(
                RevisaoEntity(
                    assuntoId = id,
                    agendadaPara = proximaRevisao.toEpochDay(),
                    status = StatusRevisao.PENDENTE.name,
                    intervaloDias = intervalo,
                    indiceCiclo = ciclo,
                ),
            )
        }

        assuntoDao.atualizarAgendamento(
            id = id,
            estadoRevisao = definirEstado(proximaRevisao).name,
            indiceCiclo = ciclo,
            proximaRevisao = proximaRevisao.toEpochDay(),
        )
    }

    override suspend fun excluirAssunto(id: Long) =
        assuntoDao.excluirPorId(id)

    override suspend fun concluirRevisao(revisaoId: Long, avaliacao: AvaliacaoRevisao) =
        database.withTransaction {
            val revisao = revisaoDao.buscarPorId(revisaoId) ?: return@withTransaction
            val data = hoje()

            revisaoDao.atualizarConclusao(
                id = revisaoId,
                concluidaEm = data.toEpochDay(),
                status = StatusRevisao.CONCLUIDA.name,
                avaliacao = avaliacao.name,
            )

            val agendamento = MotorRevisaoEspacada.proximoAgendamento(
                dataReferencia = data,
                cicloAtual = revisao.indiceCiclo,
                avaliacao = avaliacao,
                intervalos = intervalos(),
            )

            revisaoDao.inserir(
                RevisaoEntity(
                    assuntoId = revisao.assuntoId,
                    agendadaPara = agendamento.proximaRevisao.toEpochDay(),
                    status = StatusRevisao.PENDENTE.name,
                    intervaloDias = agendamento.intervaloDias,
                    indiceCiclo = agendamento.ciclo,
                ),
            )

            assuntoDao.atualizarAgendamento(
                id = revisao.assuntoId,
                estadoRevisao = definirEstado(agendamento.proximaRevisao).name,
                indiceCiclo = agendamento.ciclo,
                proximaRevisao = agendamento.proximaRevisao.toEpochDay(),
            )
        }
}
