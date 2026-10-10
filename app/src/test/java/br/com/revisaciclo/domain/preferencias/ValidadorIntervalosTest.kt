package br.com.revisaciclo.domain.preferencias

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorIntervalosTest {

    @Test
    fun `intervalos padrao sao validos`() {
        val resultado = ValidadorIntervalos.validar(listOf(1, 3, 7, 15, 30))
        assertEquals(ResultadoIntervalos.Valido(listOf(1, 3, 7, 15, 30)), resultado)
    }

    @Test
    fun `intervalo unico e valido`() {
        assertEquals(ResultadoIntervalos.Valido(listOf(5)), ValidadorIntervalos.validar(listOf(5)))
    }

    @Test
    fun `lista vazia e invalida`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.VAZIO),
            ValidadorIntervalos.validar(emptyList()),
        )
    }

    @Test
    fun `intervalo nao positivo e invalido`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_POSITIVO),
            ValidadorIntervalos.validar(listOf(0, 3)),
        )
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_POSITIVO),
            ValidadorIntervalos.validar(listOf(1, -2)),
        )
    }

    @Test
    fun `intervalo igual ao anterior e invalido`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_CRESCENTE),
            ValidadorIntervalos.validar(listOf(1, 3, 3, 7)),
        )
    }

    @Test
    fun `intervalo decrescente e invalido`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_CRESCENTE),
            ValidadorIntervalos.validar(listOf(3, 1)),
        )
    }

    @Test
    fun `intervalo acima do limite e invalido`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.EXCEDE_LIMITE),
            ValidadorIntervalos.validar(listOf(1, 4000)),
        )
    }

    @Test
    fun `quantidade de estagios acima do limite e invalida`() {
        val muitos = (1..(ValidadorIntervalos.MAX_ESTAGIOS + 1)).toList()
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.QUANTIDADE_EXCEDIDA),
            ValidadorIntervalos.validar(muitos),
        )
    }

    @Test
    fun `texto com separadores e espacos e aceito`() {
        assertEquals(
            ResultadoIntervalos.Valido(listOf(1, 3, 7, 15, 30)),
            ValidadorIntervalos.deTexto("1, 3; 7 15\t30"),
        )
    }

    @Test
    fun `texto com valor nao numerico e invalido`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.NAO_NUMERICO),
            ValidadorIntervalos.deTexto("1, tres, 7"),
        )
    }

    @Test
    fun `texto vazio e invalido`() {
        assertEquals(
            ResultadoIntervalos.Invalido(MotivoIntervaloInvalido.VAZIO),
            ValidadorIntervalos.deTexto("   "),
        )
    }

    @Test
    fun `texto com virgulas extras continua valido`() {
        assertEquals(
            ResultadoIntervalos.Valido(listOf(1, 3, 7)),
            ValidadorIntervalos.deTexto(" 1, , 3 , 7, "),
        )
    }

    @Test
    fun `formatar junta com virgula e espaco`() {
        assertEquals("1, 3, 7", ValidadorIntervalos.formatar(listOf(1, 3, 7)))
    }

    @Test
    fun `todos os motivos possuem mensagem`() {
        MotivoIntervaloInvalido.entries.forEach { motivo ->
            assertTrue(motivo.mensagem.isNotBlank())
        }
    }
}
