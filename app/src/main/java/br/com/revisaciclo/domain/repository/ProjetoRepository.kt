package br.com.revisaciclo.domain.repository

import br.com.revisaciclo.domain.model.ProjetoEstudo
import kotlinx.coroutines.flow.Flow

/**
 * Persistência dos projetos de estudo independentes.
 */
interface ProjetoRepository {
    fun observarProjetos(): Flow<List<ProjetoEstudo>>
    fun observarProjeto(id: Long): Flow<ProjetoEstudo?>

    suspend fun criar(nome: String, descricao: String?, cor: Long): Long
    suspend fun atualizar(id: Long, nome: String, descricao: String?, cor: Long)
    suspend fun definirArquivado(id: Long, arquivado: Boolean)
    suspend fun excluir(id: Long)
}
