package br.com.revisaciclo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projetos_estudo")
data class ProjetoEstudoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val descricao: String?,
    val cor: Long,
    val criadoEm: Long,
    val arquivado: Boolean,
)
