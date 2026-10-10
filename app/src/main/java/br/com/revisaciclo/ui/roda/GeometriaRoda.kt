package br.com.revisaciclo.ui.roda

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import br.com.revisaciclo.domain.model.EstagioRoda
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Anel da roda. As mesmas medidas são usadas no desenho (Canvas) e no
 * hit-test de toque, garantindo que apenas os pontos desenhados respondam a
 * toques e vice-versa.
 */
internal data class AnelRoda(
    val centro: Offset,
    val raioInterno: Float,
    val raioExterno: Float,
) {
    /** Raio nominal do círculo central do traço. */
    val raioMeio: Float
        get() = (raioInterno + raioExterno) / 2f

    val traco: Float
        get() = raioExterno - raioInterno

    /** Canto superior esquerdo do círculo do `drawArc`. */
    val topLeft: Offset
        get() = Offset(centro.x - raioMeio, centro.y - raioMeio)

    val tamanho: Size
        get() = Size(raioMeio * 2f, raioMeio * 2f)
}

/**
 * Calcula o anel para um canvas quadrado: o anel ocupa de `raioInterno` até
 * `raioExterno` (dentro dos limites do canvas, sem corte na borda).
 */
internal fun calcularAnel(tamanho: Size): AnelRoda {
    val traco = tamanho.minDimension * 0.10f
    val raioExterno = tamanho.minDimension / 2f
    val raioInterno = raioExterno - traco
    return AnelRoda(
        centro = Offset(tamanho.width / 2f, tamanho.height / 2f),
        raioInterno = raioInterno,
        raioExterno = raioExterno,
    )
}

/**
 * Distância euclidiana entre um ponto e o centro.
 */
internal fun distanciaDoCentro(centro: Offset, ponto: Offset): Float =
    sqrt((ponto.x - centro.x).pow(2) + (ponto.y - centro.y).pow(2))

/**
 * Ângulo de um ponto em graus usando a mesma convenção do `drawArc` do Compose:
 * 0° = 3 horas e ângulos positivos no sentido horário (tela com eixo Y para baixo).
 */
internal fun anguloDoPonto(centro: Offset, ponto: Offset): Float {
    val radianos = atan2(
        (ponto.y - centro.y).toDouble(),
        (ponto.x - centro.x).toDouble(),
    )
    val graus = Math.toDegrees(radianos).toFloat()
    return ((graus % 360f) + 360f) % 360f
}

/**
 * Identifica o estágio (pelo intervalo em dias) tocado em um anel desenhado com
 * segmentos contíguos a partir do topo no sentido horário, começando em -90°.
 * Retorna `null` quando o ponto está fora do anel (incluindo áreas transparentes).
 */
internal fun estagioNoPonto(
    ponto: Offset,
    tamanho: Size,
    estagios: List<EstagioRoda>,
    totalAssuntos: Int,
): Int? {
    if (totalAssuntos <= 0) return null

    val anel = calcularAnel(tamanho)
    val distancia = distanciaDoCentro(anel.centro, ponto)
    if (distancia < anel.raioInterno || distancia > anel.raioExterno) return null

    val posicao = (anguloDoPonto(anel.centro, ponto) + 90f) % 360f
    var acumulado = 0f
    estagios.forEach { estagio ->
        val varredura = 360f * estagio.total / totalAssuntos
        if (posicao >= acumulado && posicao < acumulado + varredura) {
            return estagio.intervaloDias
        }
        acumulado += varredura
    }
    return null
}