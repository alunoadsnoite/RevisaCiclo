package br.com.revisaciclo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "revisoes",
    foreignKeys = [
        ForeignKey(
            entity = AssuntoEntity::class,
            parentColumns = ["id"],
            childColumns = ["assuntoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("assuntoId")],
)
data class RevisaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assuntoId: Long,
    val agendadaPara: Long,
    val concluidaEm: Long? = null,
    val status: String,
    val intervaloDias: Int,
    val indiceCiclo: Int,
    val avaliacao: String? = null,
)
