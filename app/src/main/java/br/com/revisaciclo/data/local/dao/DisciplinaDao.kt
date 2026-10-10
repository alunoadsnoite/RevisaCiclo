package br.com.revisaciclo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.revisaciclo.data.local.entity.DisciplinaEntity
import br.com.revisaciclo.data.local.model.ResumoDisciplinaRow
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplinaDao {
    @Query("SELECT * FROM disciplinas ORDER BY nome")
    fun observarTodas(): Flow<List<DisciplinaEntity>>

    @Query("SELECT * FROM disciplinas WHERE id = :id")
    fun observarPorId(id: Long): Flow<DisciplinaEntity?>

    @Query("SELECT * FROM disciplinas WHERE id = :id")
    suspend fun buscarPorId(id: Long): DisciplinaEntity?

    @Query(
        """
        SELECT d.id AS disciplinaId,
               d.nome AS nome,
               d.cor AS cor,
               COUNT(a.id) AS totalAssuntos,
               COALESCE(
                   SUM(CASE WHEN a.proximaRevisao IS NOT NULL AND a.proximaRevisao < :hoje THEN 1 ELSE 0 END),
                   0
               ) AS assuntosAtrasados
        FROM disciplinas d
        LEFT JOIN assuntos a ON a.disciplinaId = d.id
        GROUP BY d.id
        ORDER BY d.nome
        """,
    )
    fun observarResumo(hoje: Long): Flow<List<ResumoDisciplinaRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(disciplina: DisciplinaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(disciplinas: List<DisciplinaEntity>): List<Long>

    @Query("UPDATE disciplinas SET nome = :nome, cor = :cor WHERE id = :id")
    suspend fun atualizar(id: Long, nome: String, cor: Long)

    @Query("DELETE FROM disciplinas WHERE id = :id")
    suspend fun excluirPorId(id: Long)

    @Query("SELECT COUNT(*) FROM disciplinas")
    suspend fun contar(): Int
}
