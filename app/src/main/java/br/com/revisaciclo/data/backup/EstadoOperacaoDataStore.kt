package br.com.revisaciclo.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.revisaciclo.data.preferencias.decodificarIntervalos
import br.com.revisaciclo.domain.backup.EstadoOperacao
import br.com.revisaciclo.domain.backup.RepositorioEstadoOperacao
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.TemaApp
import java.time.LocalTime
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.dataStoreOperacao: DataStore<Preferences> by preferencesDataStore(
    name = "operacao_restauracao",
)

fun criarEstadoOperacaoRepository(context: Context): RepositorioEstadoOperacao =
    EstadoOperacaoDataStore(context.applicationContext.dataStoreOperacao)

/**
 * Marcador de operação persistido em um DataStore dedicado, separado das
 * preferências do usuário.
 */
class EstadoOperacaoDataStore(
    private val dataStore: DataStore<Preferences>,
) : RepositorioEstadoOperacao {

    override suspend fun ler(): EstadoOperacao? {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        if (prefs[CHAVE_EM_ANDAMENTO] != true) return null
        val tema = prefs[CHAVE_TEMA]
            ?.let { nome -> runCatching { TemaApp.valueOf(nome) }.getOrNull() }
            ?: TemaApp.AUTOMATICO
        return EstadoOperacao(
            preferenciasPendentes = Preferencias(
                notificacoesAtivas = prefs[CHAVE_NOTIFICACOES] ?: true,
                horarioNotificacao = LocalTime.of(
                    (prefs[CHAVE_HORA] ?: 8).coerceIn(0, 23),
                    (prefs[CHAVE_MINUTO] ?: 0).coerceIn(0, 59),
                ),
                metaDiaria = (prefs[CHAVE_META] ?: Preferencias.META_DIARIA_PADRAO).coerceIn(
                    Preferencias.META_DIARIA_MINIMA,
                    Preferencias.META_DIARIA_MAXIMA,
                ),
                tema = tema,
                intervalosDias = prefs[CHAVE_INTERVALOS]?.let(::decodificarIntervalos)
                    ?: Preferencias().intervalosDias,
                projetoAtivoId = prefs[CHAVE_PROJETO_ATIVO],
            ),
        )
    }

    override suspend fun gravar(estado: EstadoOperacao) {
        val preferencias = estado.preferenciasPendentes
        dataStore.edit { prefs ->
            prefs[CHAVE_EM_ANDAMENTO] = true
            prefs[CHAVE_NOTIFICACOES] = preferencias.notificacoesAtivas
            prefs[CHAVE_HORA] = preferencias.horarioNotificacao.hour
            prefs[CHAVE_MINUTO] = preferencias.horarioNotificacao.minute
            prefs[CHAVE_META] = preferencias.metaDiaria
            prefs[CHAVE_TEMA] = preferencias.tema.name
            prefs[CHAVE_INTERVALOS] = preferencias.intervalosDias.joinToString(",")
            val projetoAtivo = preferencias.projetoAtivoId
            if (projetoAtivo == null) {
                prefs.remove(CHAVE_PROJETO_ATIVO)
            } else {
                prefs[CHAVE_PROJETO_ATIVO] = projetoAtivo
            }
        }
    }

    override suspend fun limpar() {
        dataStore.edit { prefs ->
            prefs.remove(CHAVE_EM_ANDAMENTO)
            prefs.remove(CHAVE_NOTIFICACOES)
            prefs.remove(CHAVE_HORA)
            prefs.remove(CHAVE_MINUTO)
            prefs.remove(CHAVE_META)
            prefs.remove(CHAVE_TEMA)
            prefs.remove(CHAVE_INTERVALOS)
            prefs.remove(CHAVE_PROJETO_ATIVO)
        }
    }

    private companion object {
        val CHAVE_EM_ANDAMENTO = booleanPreferencesKey("restauracao_em_andamento")
        val CHAVE_NOTIFICACOES = booleanPreferencesKey("restauracao_notificacoes")
        val CHAVE_HORA = intPreferencesKey("restauracao_hora")
        val CHAVE_MINUTO = intPreferencesKey("restauracao_minuto")
        val CHAVE_META = intPreferencesKey("restauracao_meta")
        val CHAVE_TEMA = stringPreferencesKey("restauracao_tema")
        val CHAVE_INTERVALOS = stringPreferencesKey("restauracao_intervalos")
        val CHAVE_PROJETO_ATIVO = longPreferencesKey("restauracao_projeto_ativo")
    }
}
