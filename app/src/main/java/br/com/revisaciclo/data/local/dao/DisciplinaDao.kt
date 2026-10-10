package br.com.revisaciclo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.revisaciclo.data.local.entity.DisciplinaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplinaDao {
    @Query("SELECT * FROM disciplinas ORDER BY nome")
    fun observarTodas(): Flow<List<DisciplinaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(disciplinas: List<DisciplinaEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM disciplinas")
    suspend fun contar(): Int
}
