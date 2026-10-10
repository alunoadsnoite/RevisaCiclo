package br.com.revisaciclo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "disciplinas",
    foreignKeys = [
        ForeignKey(
            entity = ProjetoEstudoEntity::class,
            parentColumns = ["id"],
            childColumns = ["projetoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projetoId")],
)
data class DisciplinaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projetoId: Long,
    val nome: String,
    val cor: Long,
    val criadaEm: Long,
)
