package br.com.revisaciclo

import br.com.revisaciclo.domain.backup.FonteArquivo
import br.com.revisaciclo.domain.model.Preferencias
import br.com.revisaciclo.domain.notificacao.AgendadorNotificacoes
import br.com.revisaciclo.domain.repository.PreferenciasRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Repositório de preferências em memória para testes instrumentados. */
class PreferenciasRepositoryFake(
    inicial: Preferencias = Preferencias(),
) : PreferenciasRepository {

    private val estado = MutableStateFlow(inicial)
    private var ultimaNotificacao: Long? = null

    override val preferencias: Flow<Preferencias> = estado.asStateFlow()

    override suspend fun atualizar(transformacao: (Preferencias) -> Preferencias) {
        estado.value = transformacao(estado.value)
    }

    override suspend fun ultimaNotificacaoDia(): Long? = ultimaNotificacao

    override suspend fun registrarNotificacaoDia(epochDay: Long) {
        ultimaNotificacao = epochDay
    }
}

/** Agendador de notificações que apenas registra as chamadas. */
class AgendadorFake : AgendadorNotificacoes {
    var reagendamentos = 0
    var cancelamentos = 0
    var ultima: Preferencias? = null

    override fun garantirAgendado(preferencias: Preferencias) = Unit

    override fun reagendar(preferencias: Preferencias) {
        reagendamentos++
        ultima = preferencias
    }

    override fun cancelar() {
        cancelamentos++
    }
}

/** Fonte de arquivo em memória (substitui o SAF nos testes). */
class FonteArquivoFake(var conteudo: ByteArray? = null) : FonteArquivo {
    var escrito: ByteArray? = null

    override suspend fun ler(identificador: String, limiteBytes: Long): ByteArray =
        conteudo ?: throw IOException("Conteúdo ausente.")

    override suspend fun escrever(identificador: String, conteudo: ByteArray) {
        this.escrito = conteudo
    }
}
