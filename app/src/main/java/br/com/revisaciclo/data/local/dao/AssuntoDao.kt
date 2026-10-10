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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(assuntos: List<AssuntoEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM assuntos")
    suspend fun contar(): Int
}
