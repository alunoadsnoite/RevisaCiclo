package br.com.revisaciclo.domain.preferencias

/**
 * Resultado da validação dos intervalos personalizados do ciclo de revisão.
 *
 * Regras: a lista não pode ser vazia; todos os valores devem ser positivos e
 * estritamente crescentes; cada valor tem um limite máximo e a quantidade de
 * estágios é limitada para manter a interface utilizável.
 */
enum class MotivoIntervaloInvalido(val mensagem: String) {
    VAZIO("Informe ao menos um intervalo."),
    NAO_NUMERICO("Use apenas números inteiros separados por vírgula."),
    NAO_POSITIVO("Os intervalos precisam ser maiores que zero."),
    NAO_CRESCENTE("Os intervalos precisam ser estritamente crescentes (ex.: 1, 3, 7)."),
    EXCEDE_LIMITE("Cada intervalo pode ter no máximo 3650 dias."),
    QUANTIDADE_EXCEDIDA("Use no máximo 20 estágios de revisão."),
}

sealed interface ResultadoIntervalos {
    data class Valido(val intervalos: List<Int>) : ResultadoIntervalos
    data class Invalido(val motivo: MotivoIntervaloInvalido) : ResultadoIntervalos
}

object ValidadorIntervalos {

    const val MAX_INTERVALO_DIAS = 3650
    const val MAX_ESTAGIOS = 20

    fun validar(intervalos: List<Int>): ResultadoIntervalos {
        if (intervalos.isEmpty()) return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.VAZIO)
        if (intervalos.size > MAX_ESTAGIOS) {
            return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.QUANTIDADE_EXCEDIDA)
        }
        if (intervalos.any { it <= 0 }) {
            return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_POSITIVO)
        }
        if (intervalos.any { it > MAX_INTERVALO_DIAS }) {
            return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.EXCEDE_LIMITE)
        }
        for (indice in 1 until intervalos.size) {
            if (intervalos[indice] <= intervalos[indice - 1]) {
                return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_CRESCENTE)
            }
        }
        return ResultadoIntervalos.Valido(intervalos)
    }

    fun deTexto(texto: String): ResultadoIntervalos {
        val partes = texto
            .split(',', ';', ' ', '\n', '\t')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (partes.isEmpty()) return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.VAZIO)

        val numeros = ArrayList<Int>(partes.size)
        for (parte in partes) {
            val numero = parte.toIntOrNull()
                ?: return ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_NUMERICO)
            numeros.add(numero)
        }
        return validar(numeros)
    }

    fun formatar(intervalos: List<Int>): String = intervalos.joinToString(", ")
}
