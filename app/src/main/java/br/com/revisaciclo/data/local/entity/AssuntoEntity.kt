package br.com.revisaciclo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assuntos",
    foreignKeys = [
        ForeignKey(
            entity = DisciplinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["disciplinaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("disciplinaId")],
)
data class AssuntoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val disciplinaId: Long,
    val nome: String,
    val dificuldade: Int,
    val criadoEm: Long,
)
