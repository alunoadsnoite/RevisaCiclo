package br.com.revisaciclo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.revisaciclo.data.local.entity.RevisaoEntity
import br.com.revisaciclo.data.local.model.ResumoDisciplinaRow
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisaoDao {
    @Query("SELECT * FROM revisoes ORDER BY agendadaPara")
    fun observarTodas(): Flow<List<RevisaoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(revisoes: List<RevisaoEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM revisoes")
    suspend fun contar(): Int

    @Query(
        """
        SELECT d.id AS disciplinaId,
               d.nome AS nome,
               d.cor AS cor,
               COUNT(r.id) AS totalRevisoes,
               COALESCE(SUM(CASE WHEN r.status = 'CONCLUIDA' THEN 1 ELSE 0 END), 0) AS concluidas
        FROM disciplinas d
        LEFT JOIN assuntos a ON a.disciplinaId = d.id
        LEFT JOIN revisoes r ON r.assuntoId = a.id
        GROUP BY d.id
        ORDER BY d.nome
        """,
    )
    fun observarResumoPorDisciplina(): Flow<List<ResumoDisciplinaRow>>
}
