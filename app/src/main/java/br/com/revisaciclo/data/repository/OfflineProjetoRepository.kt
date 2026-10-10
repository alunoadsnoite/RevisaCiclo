package br.com.revisaciclo.data.repository

import androidx.room.withTransaction
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.local.entity.ProjetoEstudoEntity
import br.com.revisaciclo.data.local.entity.toDomain
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.repository.ProjetoRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OfflineProjetoRepository(
    private val database: RevisaCicloDatabase,
    private val preferenciasRepository: PreferenciasRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ProjetoRepository {

    private val projetoDao = database.projetoEstudoDao()

    override fun observarProjetos(): Flow<List<ProjetoEstudo>> =
        projetoDao.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override fun observarProjeto(id: Long): Flow<ProjetoEstudo?> =
        projetoDao.observarPorId(id).map { it?.toDomain() }

    override suspend fun criar(nome: String, descricao: String?, cor: Long): Long =
        projetoDao.inserir(
            ProjetoEstudoEntity(
                nome = nome,
                descricao = descricao,
                cor = cor,
                criadoEm = Instant.now(clock).toEpochMilli(),
                arquivado = false,
            ),
        )

    override suspend fun atualizar(id: Long, nome: String, descricao: String?, cor: Long) =
        projetoDao.atualizar(id = id, nome = nome, descricao = descricao, cor = cor)

    override suspend fun definirArquivado(id: Long, arquivado: Boolean) =
        database.withTransaction {
            projetoDao.definirArquivado(id = id, arquivado = arquivado)
            if (arquivado) limparSeAtivo(id)
        }

    override suspend fun excluir(id: Long) =
        database.withTransaction {
            limparSeAtivo(id)
            projetoDao.excluirPorId(id)
        }

    private suspend fun limparSeAtivo(id: Long) {
        val atual = preferenciasRepository.preferencias.first().projetoAtivoId
        if (atual == id) {
            preferenciasRepository.atualizar { it.copy(projetoAtivoId = null) }
        }
    }
}
