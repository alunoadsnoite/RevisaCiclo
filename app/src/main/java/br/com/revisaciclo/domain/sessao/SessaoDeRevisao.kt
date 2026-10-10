package br.com.revisaciclo.domain.sessao

import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import java.time.LocalDate

/**
 * Revisões que devem entrar na sessão: vencidas ou agendadas para hoje.
 * A comparação é feita pela data agendada, independente do flag [RevisaoPendente.atrasada],
 * o que mantém a regra correta mesmo se houver troca de fuso ou bandeira desatualizada.
 */
fun revisoesDevidas(
    pendentes: List<RevisaoPendente>,
    hoje: LocalDate,
): List<RevisaoPendente> = pendentes.filter { !it.agendadaPara.isAfter(hoje) }

/**
 * Persiste a avaliação do item atual e só então avança a sessão.
 * A ordem é garantida: se a persistência falhar, a sessão permanece no mesmo item.
 * Retorna `false` quando não há item atual.
 */
suspend fun avaliarProximo(
    sessao: SessaoDeRevisao,
    persistir: suspend (atual: RevisaoPendente, avaliacao: AvaliacaoRevisao) -> Unit,
    avaliacao: AvaliacaoRevisao,
): Boolean {
    val atual = sessao.atual ?: return false
    persistir(atual, avaliacao)
    return sessao.avancar(avaliacao)
}

/**
 * Sessão de revisão: mantém a fila de revisões devidas e o resultado das
 * avaliações. As avaliações avancem somente um item por vez.
 */
class SessaoDeRevisao(pendentes: List<RevisaoPendente>) {

    private val fila: ArrayDeque<RevisaoPendente> = ArrayDeque(pendentes)
    private val contagens =
        linkedMapOf<AvaliacaoRevisao, Int>().apply {
            AvaliacaoRevisao.entries.forEach { avaliacao -> put(avaliacao, 0) }
        }

    val total: Int = pendentes.size

    val atual: RevisaoPendente?
        get() = fila.firstOrNull()

    val concluida: Boolean
        get() = fila.isEmpty()

    val concluidas: Int
        get() = total - fila.size

    val contagemResultados: Map<AvaliacaoRevisao, Int>
        get() = contagens

    /**
     * Remove o item atual da fila e acumula a avaliação aplicada.
     * Retorna `false` quando não há item pendente.
     */
    fun avancar(avaliacao: AvaliacaoRevisao): Boolean {
        if (fila.isEmpty()) return false
        fila.removeFirst()
        contagens[avaliacao] = (contagens[avaliacao] ?: 0) + 1
        return true
    }
}