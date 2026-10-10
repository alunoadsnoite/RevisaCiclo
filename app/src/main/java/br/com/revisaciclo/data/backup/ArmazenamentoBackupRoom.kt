package br.com.revisaciclo.data.backup

import androidx.room.withTransaction
import br.com.revisaciclo.data.local.RevisaCicloDatabase
import br.com.revisaciclo.data.local.entity.toDomain
import br.com.revisaciclo.data.local.entity.toEntity
import br.com.revisaciclo.domain.backup.ArmazenamentoBackup
import br.com.revisaciclo.domain.backup.BackupDados
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.first

/**
 * Implementação de [ArmazenamentoBackup] sobre Room + DataStore.
 *
 * - `capturar`: lê todas as tabelas dentro de uma transação de leitura (snapshot
 *   consistente entre elas) e, em seguida, lê as preferências do DataStore.
 * - `substituirRoom`: apaga e reinsere tudo dentro de uma transação; a ordem de
 *   apagar (filhos antes de pais) e inserir (pais antes de filhos) respeita as
 *   chaves estrangeiras, e os IDs originais são preservados.
 */
class ArmazenamentoBackupRoom(
    private val database: RevisaCicloDatabase,
    private val preferenciasRepository: PreferenciasRepository,
) : ArmazenamentoBackup {

    override suspend fun capturar(): BackupDados {
        val dados = database.withTransaction {
            BackupDados(
                projetos = database.projetoEstudoDao().listarTodos().map { it.toDomain() },
                disciplinas = database.disciplinaDao().listarTodas().map { it.toDomain() },
                assuntos = database.assuntoDao().listarTodos().map { it.toDomain() },
                revisoes = database.revisaoDao().listarTodas().map { it.toDomain() },
                preferencias = Preferencias(),
            )
        }
        return dados.copy(preferencias = lerPreferencias())
    }

    override suspend fun substituirRoom(dados: BackupDados) {
        database.withTransaction {
            val revisoes = database.revisaoDao()
            val assuntos = database.assuntoDao()
            val disciplinas = database.disciplinaDao()
            val projetos = database.projetoEstudoDao()

            revisoes.limpar()
            assuntos.limpar()
            disciplinas.limpar()
            projetos.limpar()

            if (dados.projetos.isNotEmpty()) projetos.inserir(dados.projetos.map { it.toEntity() })
            if (dados.disciplinas.isNotEmpty()) {
                disciplinas.inserir(dados.disciplinas.map { it.toEntity() })
            }
            if (dados.assuntos.isNotEmpty()) assuntos.inserir(dados.assuntos.map { it.toEntity() })
            if (dados.revisoes.isNotEmpty()) revisoes.inserir(dados.revisoes.map { it.toEntity() })
        }
    }

    override suspend fun aplicarPreferencias(preferencias: Preferencias) {
        preferenciasRepository.atualizar { preferencias }
    }

    override suspend fun lerPreferencias(): Preferencias =
        preferenciasRepository.preferencias.first()
}
