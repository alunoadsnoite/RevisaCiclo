package br.com.revisaciclo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.revisaciclo.data.local.entity.AssuntoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssuntoDao {
    @Query("SELECT * FROM assuntos ORDER BY nome")
    fun observarTodos(): Flow<List<AssuntoEntity>>

    @Query("SELECT * FROM assuntos ORDER BY id")
    suspend fun listarTodos(): List<AssuntoEntity>

    @Query("SELECT * FROM assuntos WHERE disciplinaId = :disciplinaId ORDER BY nome")
    fun observarPorDisciplina(disciplinaId: Long): Flow<List<AssuntoEntity>>

    @Query("SELECT * FROM assuntos WHERE id = :id")
    fun observarPorId(id: Long): Flow<AssuntoEntity?>

    @Query("SELECT * FROM assuntos WHERE id = :id")
    suspend fun buscarPorId(id: Long): AssuntoEntity?

    @Query("DELETE FROM assuntos")
    suspend fun limpar()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(assunto: AssuntoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(assuntos: List<AssuntoEntity>): List<Long>

    @Query("UPDATE assuntos SET disciplinaId = :disciplinaId, nome = :titulo, descricao = :descricao, dataEstudo = :dataEstudo WHERE id = :id")
    suspend fun atualizar(
        id: Long,
        disciplinaId: Long,
        titulo: String,
        descricao: String?,
        dataEstudo: Long,
    )

    @Query("UPDATE assuntos SET estadoRevisao = :estadoRevisao, indiceCiclo = :indiceCiclo, proximaRevisao = :proximaRevisao WHERE id = :id")
    suspend fun atualizarAgendamento(
        id: Long,
        estadoRevisao: String,
        indiceCiclo: Int,
        proximaRevisao: Long,
    )

    @Query("DELETE FROM assuntos WHERE id = :id")
    suspend fun excluirPorId(id: Long)

    @Query("SELECT COUNT(*) FROM assuntos")
    suspend fun contar(): Int
}
