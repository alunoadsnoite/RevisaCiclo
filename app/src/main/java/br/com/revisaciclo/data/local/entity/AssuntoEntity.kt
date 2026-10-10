package br.com.revisaciclo.data.local.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "nome") val titulo: String,
    val descricao: String? = null,
    val dataEstudo: Long = 0,
    val estadoRevisao: String = "NOVO",
    val indiceCiclo: Int = 0,
    val proximaRevisao: Long? = null,
)
