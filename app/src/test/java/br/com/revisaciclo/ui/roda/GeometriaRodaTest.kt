package br.com.revisaciclo.ui.roda

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import br.com.revisaciclo.domain.model.AssuntoRoda
import br.com.revisaciclo.domain.model.EstagioRoda
import br.com.revisaciclo.domain.model.SituacaoRevisao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeometriaRodaTest {

    private val tamanho = Size(200f, 200f)

    private fun estagio(intervalo: Int) = EstagioRoda(
        intervaloDias = intervalo,
        assuntos = listOf(
            AssuntoRoda(
                id = 1L,
                titulo = "A",
                disciplinaNome = "M",
                disciplinaCor = 0xFF000000,
                intervaloDias = intervalo,
                proximaRevisao = null,
                situacao = SituacaoRevisao.FUTURO,
            ),
        ),
    )

    @Test
    fun `tocar no topo seleciona o primeiro estagio`() {
        val selecionado = estagioNoPonto(
            ponto = Offset(100f, 20f),
            tamanho = tamanho,
            estagios = listOf(estagio(1), estagio(3)),
            totalAssuntos = 2,
        )

        assertEquals(1, selecionado)
    }

    @Test
    fun `tocar embaixo seleciona o segundo estagio`() {
        val selecionado = estagioNoPonto(
            ponto = Offset(100f, 180f),
            tamanho = tamanho,
            estagios = listOf(estagio(1), estagio(3)),
            totalAssuntos = 2,
        )

        assertEquals(3, selecionado)
    }

    @Test
    fun `tocar no centro ou fora do anel retorna nulo`() {
        val estagios = listOf(estagio(1), estagio(3))
        assertNull(estagioNoPonto(Offset(100f, 100f), tamanho, estagios, 2))
        assertNull(estagioNoPonto(Offset(0f, 0f), tamanho, estagios, 2))
        assertNull(estagioNoPonto(Offset(200f, 200f), tamanho, estagios, 2))
    }

    @Test
    fun `anel compartilha as mesmas medidas entre desenho e toque`() {
        val anel = calcularAnel(tamanho)

        assertEquals(80f, anel.raioInterno, 0.001f)
        assertEquals(100f, anel.raioExterno, 0.001f)
        assertEquals(90f, anel.raioMeio, 0.001f)
        assertEquals(20f, anel.traco, 0.001f)
        // o círculo desenhado tem centro no centro do canvas e cabe dentro dele
        assertEquals(100f, anel.centro.x, 0.001f)
        assertEquals(100f, anel.centro.y, 0.001f)
        assertEquals(180f, anel.tamanho.width, 0.001f)
        assertEquals(180f, anel.tamanho.height, 0.001f)
        assertEquals(10f, anel.topLeft.x, 0.001f)
        assertEquals(10f, anel.topLeft.y, 0.001f)
    }

    @Test
    fun `toque na borda interna seleciona estagio`() {
        val selecionado = estagioNoPonto(
            ponto = Offset(180f, 100f),
            tamanho = tamanho,
            estagios = listOf(estagio(1), estagio(3)),
            totalAssuntos = 2,
        )

        assertEquals(1, selecionado)
    }

    @Test
    fun `toque dentro do limite interno retorna nulo`() {
        val estagios = listOf(estagio(1), estagio(3))
        assertNull(estagioNoPonto(Offset(179f, 100f), tamanho, estagios, 2))
    }

    @Test
    fun `toque na borda externa seleciona estagio`() {
        val selecionado = estagioNoPonto(
            ponto = Offset(200f, 100f),
            tamanho = tamanho,
            estagios = listOf(estagio(1), estagio(3)),
            totalAssuntos = 2,
        )

        assertEquals(1, selecionado)
    }

    @Test
    fun `tolera ponto fora do raio externo dentro do canvas`() {
        val estagios = listOf(estagio(1), estagio(3))
        assertNull(estagioNoPonto(Offset(15f, 15f), tamanho, estagios, 2))
        assertNull(estagioNoPonto(Offset(185f, 185f), tamanho, estagios, 2))
    }

    @Test
    fun `regiao antes transparente do anel agora esta no anel`() {
        // raio 85 ficava entre a borda visivel antiga e a nova borda interna;
        // com a geometria compartilhada ele faz parte do anel e seleciona.
        val selecionado = estagioNoPonto(
            ponto = Offset(185f, 100f),
            tamanho = tamanho,
            estagios = listOf(estagio(1), estagio(3)),
            totalAssuntos = 2,
        )

        assertEquals(1, selecionado)
    }
}