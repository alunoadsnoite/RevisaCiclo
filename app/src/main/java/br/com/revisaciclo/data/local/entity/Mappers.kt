package br.com.revisaciclo.data.local.entity

import br.com.revisaciclo.domain.model.Assunto
import br.com.revisaciclo.domain.model.Disciplina
import br.com.revisaciclo.domain.model.Revisao
import br.com.revisaciclo.domain.model.StatusRevisao

fun DisciplinaEntity.toDomain(): Disciplina =
    Disciplina(id = id, nome = nome, cor = cor, criadaEm = criadaEm)

fun AssuntoEntity.toDomain(): Assunto =
    Assunto(
        id = id,
        disciplinaId = disciplinaId,
        nome = nome,
        dificuldade = dificuldade,
        criadoEm = criadoEm,
    )

fun RevisaoEntity.toDomain(): Revisao =
    Revisao(
        id = id,
        assuntoId = assuntoId,
        agendadaPara = agendadaPara,
        concluidaEm = concluidaEm,
        status = runCatching { StatusRevisao.valueOf(status) }.getOrDefault(StatusRevisao.PENDENTE),
        intervaloDias = intervaloDias,
        indiceCiclo = indiceCiclo,
        qualidade = qualidade,
    )
