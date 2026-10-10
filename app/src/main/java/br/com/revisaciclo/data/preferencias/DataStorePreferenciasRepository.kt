package br.com.revisaciclo.data.preferencias

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.model.TemaApp
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import br.com.revisaciclo.domain.scheduler.MotorRevisaoEspacada
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "revisaciclo_preferencias",
)

fun criarPreferenciasRepository(context: Context): PreferenciasRepository =
    DataStorePreferenciasRepository(context.applicationContext.dataStore)

/**
 * Implementação de [PreferenciasRepository] sobre Jetpack DataStore.
 *
 * Recebe o [DataStore] por injeção para permitir testes com arquivo temporário.
 */
class DataStorePreferenciasRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferenciasRepository {

    override val preferencias: Flow<Preferencias> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it.toPreferencias() }

    override suspend fun atualizar(transformacao: (Preferencias) -> Preferencias) {
        dataStore.edit { preferencias ->
            preferencias.escrever(transformacao(preferencias.toPreferencias()))
        }
    }

    override suspend fun ultimaNotificacaoDia(): Long? =
        dataStore.data.catch { emit(emptyPreferences()) }.first()[CHAVE_ULTIMA_NOTIFICACAO]

    override suspend fun registrarNotificacaoDia(epochDay: Long) {
        dataStore.edit { it[CHAVE_ULTIMA_NOTIFICACAO] = epochDay }
    }
}

internal fun Preferences.toPreferencias(): Preferencias {
    val tema = this[CHAVE_TEMA]
        ?.let { nome -> runCatching { TemaApp.valueOf(nome) }.getOrNull() }
        ?: TemaApp.AUTOMATICO
    val hora = (this[CHAVE_HORA] ?: HORARIO_PADRAO.hour).coerceIn(0, 23)
    val minuto = (this[CHAVE_MINUTO] ?: HORARIO_PADRAO.minute).coerceIn(0, 59)
    val intervalos = this[CHAVE_INTERVALOS]?.let(::decodificarIntervalos)
        ?: MotorRevisaoEspacada.INTERVALOS_PADRAO

    return Preferencias(
        notificacoesAtivas = this[CHAVE_NOTIFICACOES] ?: true,
        horarioNotificacao = LocalTime.of(hora, minuto),
        metaDiaria = (this[CHAVE_META] ?: Preferencias.META_DIARIA_PADRAO)
            .coerceIn(Preferencias.META_DIARIA_MINIMA, Preferencias.META_DIARIA_MAXIMA),
        tema = tema,
        intervalosDias = intervalos,
    )
}

internal fun MutablePreferences.escrever(preferencias: Preferencias) {
    this[CHAVE_NOTIFICACOES] = preferencias.notificacoesAtivas
    this[CHAVE_HORA] = preferencias.horarioNotificacao.hour
    this[CHAVE_MINUTO] = preferencias.horarioNotificacao.minute
    this[CHAVE_META] = preferencias.metaDiaria
    this[CHAVE_TEMA] = preferencias.tema.name
    this[CHAVE_INTERVALOS] = preferencias.intervalosDias.joinToString(",")
}

internal fun decodificarIntervalos(texto: String): List<Int> =
    texto.split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .takeIf { it.isNotEmpty() }
        ?: MotorRevisaoEspacada.INTERVALOS_PADRAO

private val HORARIO_PADRAO: LocalTime = LocalTime.of(8, 0)

private val CHAVE_NOTIFICACOES = booleanPreferencesKey("notificacoes_ativas")
private val CHAVE_HORA = intPreferencesKey("horario_hora")
private val CHAVE_MINUTO = intPreferencesKey("horario_minuto")
private val CHAVE_META = intPreferencesKey("meta_diaria")
private val CHAVE_TEMA = stringPreferencesKey("tema")
private val CHAVE_INTERVALOS = stringPreferencesKey("intervalos_dias")
private val CHAVE_ULTIMA_NOTIFICACAO = longPreferencesKey("ultima_notificacao_dia")
