package br.com.revisaciclo.domain.repository

import br.com.revisaciclo.domain.model.Preferencias
import kotlinx.coroutines.flow.Flow

/**
 * Persistência das preferências do usuário (DataStore).
 *
 * O estado operacional de notificação (último dia notificado) é exposto em
 * separado para não misturar preferência com controle interno.
 */
interface PreferenciasRepository {
    val preferencias: Flow<Preferencias>

    suspend fun atualizar(transformacao: (Preferencias) -> Preferencias)

    suspend fun ultimaNotificacaoDia(): Long?

    suspend fun registrarNotificacaoDia(epochDay: Long)
}
