package br.com.revisaciclo.data.local.entity

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.AvaliacaoRevisao
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.EstadoRevisao
import br.com.revisaciclo.domain.model.ProjetoEstudo
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao
import java.time.LocalDate

fun DisciplinaEntity.toDomain(): Disciplina =
    Disciplina(id = id, projetoId = projetoId, nome = nome, cor = cor, criadaEm = criadaEm)

fun ProjetoEstudoEntity.toDomain(): ProjetoEstudo =
    ProjetoEstudo(
        id = id,
        nome = nome,
        descricao = descricao,
        cor = cor,
        criadoEm = criadoEm,
        arquivado = arquivado,
    )

fun AssuntoEntity.toDomain(): Assunto =
    Assunto(
        id = id,
        disciplinaId = disciplinaId,
        titulo = titulo,
        descricao = descricao,
        dataEstudo = LocalDate.ofEpochDay(dataEstudo),
        estadoRevisao = runCatching { EstadoRevisao.valueOf(estadoRevisao) }
            .getOrDefault(EstadoRevisao.NOVO),
        indiceCiclo = indiceCiclo,
        proximaRevisao = proximaRevisao?.let { LocalDate.ofEpochDay(it) },
    )

fun RevisaoEntity.toDomain(): Revisao =
    Revisao(
        id = id,
        assuntoId = assuntoId,
        agendadaPara = LocalDate.ofEpochDay(agendadaPara),
        concluidaEm = concluidaEm?.let { LocalDate.ofEpochDay(it) },
        status = runCatching { StatusRevisao.valueOf(status) }
            .getOrDefault(StatusRevisao.PENDENTE),
        intervaloDias = intervaloDias,
        indiceCiclo = indiceCiclo,
        avaliacao = avaliacao?.let { runCatching { AvaliacaoRevisao.valueOf(it) }.getOrNull() },
    )

fun LocalDate.toEpochDayLong(): Long = toEpochDay()
