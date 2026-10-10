package br.com.revisaciclo.domain.agregacao

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.ProgressoDisciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.RevisaoPendente
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroProjetoTest {

    private fun disciplina(id: Long, projetoId: Long) = Disciplina(
        id = id,
        projetoId = projetoId,
        nome = "D$id",
        cor = 0xFF1565C0,
        criadaEm = 1L,
    )

    private fun assunto(id: Long, disciplinaId: Long) = Assunto(
        id = id,
        disciplinaId = disciplinaId,
        titulo = "A$id",
        descricao = null,
        dataEstudo = LocalDate.of(2026, 1, 1),
        estadoRevisao = EstadoRevisao.EM_DIA,
        indiceCiclo = 1,
        proximaRevisao = LocalDate.of(2026, 1, 2),
    )

    private fun revisao(id: Long, assuntoId: Long) = Revisao(
        id = id,
        assuntoId = assuntoId,
        agendadaPara = LocalDate.of(2026, 1, 2),
        concluidaEm = null,
        status = StatusRevisao.PENDENTE,
        intervaloDias = 1,
        indiceCiclo = 1,
        avaliacao = null,
    )

    @Test
    fun `disciplinas sao filtradas pelo projeto`() {
        val todas = listOf(disciplina(1, projetoId = 10), disciplina(2, projetoId = 20))

        assertEquals(listOf(1L), FiltroProjeto.disciplinas(todas, 10).map { it.id })
        assertEquals(listOf(2L), FiltroProjeto.disciplinas(todas, 20).map { it.id })
        assertTrue(FiltroProjeto.disciplinas(todas, null).isEmpty())
        assertTrue(FiltroProjeto.disciplinas(todas, 99).isEmpty())
    }

    @Test
    fun `assuntos revisoes e progresso descendem das disciplinas`() {
        val disciplinas = FiltroProjeto.disciplinas(
            listOf(disciplina(1, 10), disciplina(2, 20)),
            projetoId = 10,
        )
        val disciplinaIds = disciplinas.map { it.id }.toSet()

        val assuntos = FiltroProjeto.assuntos(
            listOf(assunto(100, 1), assunto(200, 2)),
            disciplinaIds,
        )
        assertEquals(listOf(100L), assuntos.map { it.id })

        val assuntoIds = assuntos.map { it.id }.toSet()
        val revisoes = FiltroProjeto.revisoes(
            listOf(revisao(1, 100), revisao(2, 200)),
            assuntoIds,
        )
        assertEquals(listOf(1L), revisoes.map { it.id })

        val progressos = FiltroProjeto.progresso(
            listOf(
                ProgressoDisciplina(1, "D1", 0, 0, 0),
                ProgressoDisciplina(2, "D2", 0, 0, 0),
            ),
            disciplinaIds,
        )
        assertEquals(listOf(1L), progressos.map { it.disciplinaId })
    }

    @Test
    fun `revisoes pendentes sao filtradas pela disciplina`() {
        val pendentes = listOf(
            pendente(revisaoId = 1, disciplinaId = 1),
            pendente(revisaoId = 2, disciplinaId = 2),
        )

        val filtradas = FiltroProjeto.revisoesPendentes(pendentes, setOf(2L))

        assertEquals(listOf(2L), filtradas.map { it.revisaoId })
    }

    private fun pendente(revisaoId: Long, disciplinaId: Long) = RevisaoPendente(
        revisaoId = revisaoId,
        assuntoId = revisaoId,
        assuntoTitulo = "A$revisaoId",
        disciplinaId = disciplinaId,
        disciplinaNome = "D$disciplinaId",
        disciplinaCor = 0xFF1565C0,
        agendadaPara = LocalDate.of(2026, 1, 2),
        indiceCiclo = 1,
        intervaloDias = 1,
        atrasada = false,
    )
}
