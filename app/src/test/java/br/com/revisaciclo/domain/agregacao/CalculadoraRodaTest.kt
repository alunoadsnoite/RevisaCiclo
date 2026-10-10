package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.SituacaoRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculadoraRodaTest {

    private val hoje = LocalDate.of(2026, 1, 10)

    private fun disciplina(
        id: Long,
        nome: String = "Matemática",
        cor: Long = 0xFF1565C0,
    ) = Disciplina(id = id, nome = nome, cor = cor, criadaEm = 1L)

    private fun assunto(
        id: Long,
        disciplinaId: Long = 1L,
        titulo: String = "Assunto $id",
        indiceCiclo: Int = 1,
        proximaRevisao: LocalDate? = hoje,
    ) = Assunto(
        id = id,
        disciplinaId = disciplinaId,
        titulo = titulo,
        descricao = null,
        dataEstudo = LocalDate.of(2026, 1, 1),
        estadoRevisao = EstadoRevisao.EM_DIA,
        indiceCiclo = indiceCiclo,
        proximaRevisao = proximaRevisao,
    )

    @Test
    fun `distribui assuntos pelos cinco estagios conforme o ciclo`() {
        val assuntos = listOf(
            assunto(1, indiceCiclo = 1),
            assunto(2, indiceCiclo = 2),
            assunto(3, indiceCiclo = 3),
            assunto(4, indiceCiclo = 4),
            assunto(5, indiceCiclo = 5),
        )

        val roda = CalculadoraRoda.montar(assuntos, emptyMap(), hoje)

        assertEquals(5, roda.estagios.size)
        assertEquals(listOf(1, 3, 7, 15, 30), roda.estagios.map { it.intervaloDias })
        roda.estagios.forEach { estagio ->
            assertEquals(1, estagio.total)
            assertEquals(estagio.intervaloDias, estagio.assuntos.first().intervaloDias)
        }
        assertEquals(5, roda.totalAssuntos)
    }

    @Test
    fun `situacao da revisao e derivada da proxima data`() {
        val atrasado = assunto(1, proximaRevisao = hoje.minusDays(1))
        val hojeRev = assunto(2, proximaRevisao = hoje)
        val futuro = assunto(3, proximaRevisao = hoje.plusDays(2))
        val semData = assunto(4, proximaRevisao = null)

        val roda = CalculadoraRoda.montar(listOf(semData, futuro, hojeRev, atrasado), emptyMap(), hoje)
        val porSituacao = roda.estagios
            .flatMap { it.assuntos }
            .associateBy { it.id }

        assertEquals(SituacaoRevisao.ATRASADO, porSituacao.getValue(1).situacao)
        assertEquals(SituacaoRevisao.HOJE, porSituacao.getValue(2).situacao)
        assertEquals(SituacaoRevisao.FUTURO, porSituacao.getValue(3).situacao)
        assertEquals(SituacaoRevisao.FUTURO, porSituacao.getValue(4).situacao)
    }

    @Test
    fun `estagio conta atrasados e vencendo hoje`() {
        val assuntos = listOf(
            assunto(1, proximaRevisao = hoje.minusDays(1)),
            assunto(2, proximaRevisao = hoje.minusDays(1)),
            assunto(3, proximaRevisao = hoje),
            assunto(4, proximaRevisao = hoje.plusDays(5)),
        )

        val roda = CalculadoraRoda.montar(assuntos, emptyMap(), hoje)
        val estagio1 = roda.estagios.first { it.intervaloDias == 1 }

        assertEquals(4, estagio1.total)
        assertEquals(2, estagio1.atrasados)
        assertEquals(1, estagio1.hoje)
    }

    @Test
    fun `disciplina ausente usa valores padrao`() {
        val roda = CalculadoraRoda.montar(listOf(assunto(1)), emptyMap(), hoje)
        val item = roda.estagios.first().assuntos.single()

        assertEquals("Sem disciplina", item.disciplinaNome)
        assertEquals(0xFF9E9E9E, item.disciplinaCor)
    }

    @Test
    fun `usuario disciplina informa nome e cor`() {
        val aula = disciplina(10, nome = "Português", cor = 0xFF2E7D32)
        val roda = CalculadoraRoda.montar(listOf(assunto(1, disciplinaId = 10)), mapOf(10L to aula), hoje)
        val item = roda.estagios.first().assuntos.single()

        assertEquals("Português", item.disciplinaNome)
        assertEquals(0xFF2E7D32, item.disciplinaCor)
    }

    @Test
    fun `lista vazia produz roda vazia com estagios zerados`() {
        val roda = CalculadoraRoda.montar(emptyList(), emptyMap(), hoje)

        assertTrue(roda.estaVazia)
        assertEquals(0, roda.totalAssuntos)
        assertEquals(5, roda.estagios.size)
        roda.estagios.forEach { assertEquals(0, it.total) }
    }

    @Test
    fun `assunto que vence hoje fica atrasado na virada do dia`() {
        val dia = LocalDate.of(2026, 1, 10)
        val assuntoDia = assunto(1, indiceCiclo = 1, proximaRevisao = dia)

        val rodaDoDia = CalculadoraRoda.montar(listOf(assuntoDia), emptyMap(), dia)
        assertEquals(SituacaoRevisao.HOJE, rodaDoDia.estagios.first().assuntos.single().situacao)

        val rodaSeguinte = CalculadoraRoda.montar(listOf(assuntoDia), emptyMap(), dia.plusDays(1))
        assertEquals(SituacaoRevisao.ATRASADO, rodaSeguinte.estagios.first().assuntos.single().situacao)
    }

    @Test
    fun `assunto agendado para amanha passa a vencer hoje na virada`() {
        val dia = LocalDate.of(2026, 1, 10)
        val amanha = dia.plusDays(1)
        val assuntoAmanha = assunto(1, indiceCiclo = 1, proximaRevisao = amanha)

        val rodaHoje = CalculadoraRoda.montar(listOf(assuntoAmanha), emptyMap(), dia)
        assertEquals(SituacaoRevisao.FUTURO, rodaHoje.estagios.first().assuntos.single().situacao)

        val rodaAmanha = CalculadoraRoda.montar(listOf(assuntoAmanha), emptyMap(), amanha)
        assertEquals(SituacaoRevisao.HOJE, rodaAmanha.estagios.first().assuntos.single().situacao)
    }
}