package br.com.revisaciclo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.revisaciclo.data.local.entity.RevisaoEntity
import br.com.revisaciclo.data.local.model.RevisaoPendenteRow
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisaoDao {
    @Query("SELECT * FROM revisoes ORDER BY agendadaPara")
    fun observarTodas(): Flow<List<RevisaoEntity>>

    @Query(
        """
        SELECT r.id AS revisaoId,
               a.id AS assuntoId,
               a.nome AS assuntoTitulo,
               a.descricao AS assuntoDescricao,
               d.id AS disciplinaId,
               d.nome AS disciplinaNome,
               d.cor AS disciplinaCor,
               r.agendadaPara AS agendadaPara,
               r.indiceCiclo AS indiceCiclo,
               r.intervaloDias AS intervaloDias
        FROM revisoes r
        JOIN assuntos a ON a.id = r.assuntoId
        JOIN disciplinas d ON d.id = a.disciplinaId
        WHERE r.status != 'CONCLUIDA'
        ORDER BY r.agendadaPara
        """,
    )
    fun observarPendentes(): Flow<List<RevisaoPendenteRow>>

    @Query("SELECT * FROM revisoes WHERE id = :id")
    suspend fun buscarPorId(id: Long): RevisaoEntity?

    @Query("SELECT * FROM revisoes WHERE assuntoId = :assuntoId AND status != 'CONCLUIDA' ORDER BY indiceCiclo DESC LIMIT 1")
    suspend fun buscarPendentePorAssunto(assuntoId: Long): RevisaoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(revisao: RevisaoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(revisoes: List<RevisaoEntity>): List<Long>

    @Query("UPDATE revisoes SET concluidaEm = :concluidaEm, status = :status, avaliacao = :avaliacao WHERE id = :id")
    suspend fun atualizarConclusao(
        id: Long,
        concluidaEm: Long,
        status: String,
        avaliacao: String,
    )

    @Query("UPDATE revisoes SET agendadaPara = :agendadaPara WHERE id = :id")
    suspend fun atualizarAgendadaPara(id: Long, agendadaPara: Long)

    @Query("SELECT COUNT(*) FROM revisoes")
    suspend fun contar(): Int
}
