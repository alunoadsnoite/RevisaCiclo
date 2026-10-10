package br.com.revisaciclo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.revisaciclo.data.local.entity.ProjetoEstudoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjetoEstudoDao {
    @Query("SELECT * FROM projetos_estudo ORDER BY nome")
    fun observarTodos(): Flow<List<ProjetoEstudoEntity>>

    @Query("SELECT * FROM projetos_estudo ORDER BY id")
    suspend fun listarTodos(): List<ProjetoEstudoEntity>

    @Query("SELECT * FROM projetos_estudo WHERE id = :id")
    fun observarPorId(id: Long): Flow<ProjetoEstudoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(projeto: ProjetoEstudoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(projetos: List<ProjetoEstudoEntity>): List<Long>

    @Query("DELETE FROM projetos_estudo")
    suspend fun limpar()

    @Query("UPDATE projetos_estudo SET nome = :nome, descricao = :descricao, cor = :cor WHERE id = :id")
    suspend fun atualizar(id: Long, nome: String, descricao: String?, cor: Long)

    @Query("UPDATE projetos_estudo SET arquivado = :arquivado WHERE id = :id")
    suspend fun definirArquivado(id: Long, arquivado: Boolean)

    @Query("DELETE FROM projetos_estudo WHERE id = :id")
    suspend fun excluirPorId(id: Long)

    @Query("SELECT COUNT(*) FROM projetos_estudo")
    suspend fun contar(): Int
}
