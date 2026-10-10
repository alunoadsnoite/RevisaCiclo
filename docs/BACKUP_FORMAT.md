# Formato de backup do RevisaCiclo

Este documento descreve o formato de arquivo usado pelo RevisaCiclo para
**backup completo** (restaurável) e para a **exportação portátil** (JSON aberto).

## Visão geral

- Extensão: `.revisaciclo`
- Conteúdo: texto JSON (UTF-8), **sem** criptografia. Pode conter dados pessoais
  de estudo; guarde com cuidado.
- Tamanho máximo aceito na restauração: 25 MiB.
- O arquivo não guarda caminhos nem permissões do sistema: pode ser movido,
  copiado ou enviado por e-mail/Drive e restaurado em outro dispositivo.

## Backup completo (`.revisaciclo`)

Estrutura em duas camadas:

```
{
  "formato":          "revisaciclo-backup",
  "versaoFormato":    1,
  "versaoApp":        "0.1.0",
  "versaoSchemaBanco": 3,
  "criadoEm":         "2026-10-10T10:00:00Z",
  "checksum": {
    "algoritmo": "SHA-256",
    "valor":    "<hex de 64 caracteres>"
  },
  "contagens": {
    "projetos":    1,
    "disciplinas": 2,
    "assuntos":    3,
    "revisoes":    4
  },
  "payload": { ... }
}
```

### Campos do cabeçalho

| Campo | Descrição |
|---|---|
| `formato` | Identificador fixo de formato: `revisaciclo-backup`. |
| `versaoFormato` | Versão do formato (atual: `1`). Arquivos de formato mais novo são rejeitados. |
| `versaoApp` | Versão do aplicativo que gerou o arquivo (apenas informativa). |
| `versaoSchemaBanco` | Versão do schema Room no momento da exportação (atual: `3`). |
| `criadoEm` | Momento da exportação, em `Instant` ISO-8601 (UTC). |
| `checksum.algoritmo` | `SHA-256` (fixo). |
| `checksum.valor` | SHA-256 em hex minúsculo do **payload serializado de forma canônica**. |
| `contagens` | Quantidade de registros em cada coleção, usada para conferência na restauração. |

A integridade é verificada **antes** de qualquer gravação: o aplicativo recalcula o
SHA-256 da serialização canônica do `payload` e compara com `checksum.valor`.
Canônica significa chaves ordenadas e sem espaços extras, o que torna a verificação
determinística independentemente de formatação.

## Payload

Cinco coleções. Datas de calendário são strings **ISO-8601** (`AAAA-MM-DD`);
`criadoEm`/`criadaEm` são **epoch millis** como inteiros.

### `projetos`

| Campo | Tipo | Observação |
|---|---|---|
| `id` | inteiro positivo | único, preservado na restauração |
| `nome` | texto | obrigatório |
| `descricao` | texto ou `null` | opcional |
| `cor` | inteiro (ARGB) | |
| `criadoEm` | inteiro (epoch millis) | |
| `arquivado` | booleano | |

### `disciplinas`

| Campo | Tipo | Observação |
|---|---|---|
| `id` | inteiro positivo | único |
| `projetoId` | inteiro | FK → `projetos.id` |
| `nome` | texto | obrigatório |
| `cor` | inteiro (ARGB) | |
| `criadaEm` | inteiro (epoch millis) | |

### `assuntos`

| Campo | Tipo | Observação |
|---|---|---|
| `id` | inteiro positivo | único |
| `disciplinaId` | inteiro | FK → `disciplinas.id` |
| `titulo` | texto | obrigatório |
| `descricao` | texto ou `null` | opcional |
| `dataEstudo` | `AAAA-MM-DD` | |
| `estadoRevisao` | enum `NOVO` / `EM_DIA` / `ATRASADO` | |
| `indiceCiclo` | inteiro ≥ 0 | estágio do ciclo |
| `proximaRevisao` | `AAAA-MM-DD` ou `null` | |

### `revisoes`

| Campo | Tipo | Observação |
|---|---|---|
| `id` | inteiro positivo | único |
| `assuntoId` | inteiro | FK → `assuntos.id` |
| `agendadaPara` | `AAAA-MM-DD` | |
| `concluidaEm` | `AAAA-MM-DD` ou `null` | |
| `status` | enum `PENDENTE` / `CONCLUIDA` / `ATRASADA` | |
| `intervaloDias` | inteiro > 0 | |
| `indiceCiclo` | inteiro ≥ 0 | |
| `avaliacao` | `null` ou `ESQUECI` / `DIFICIL` / `BOM` / `FACIL` | |

### `preferencias`

| Campo | Tipo | Observação |
|---|---|---|
| `notificacoesAtivas` | booleano | |
| `horarioHora` | inteiro 0..23 | |
| `horarioMinuto` | inteiro 0..59 | |
| `metaDiaria` | inteiro 1..200 | |
| `tema` | enum `CLARO` / `ESCURO` / `AUTOMATICO` | |
| `intervalosDias` | lista de inteiros | crescente, positivo; máximo 20 estágios / 3650 dias |
| `projetoAtivoId` | inteiro ou `null` | FK → `projetos.id`; pode ser `null` |

> A chave operacional interna `ultimaNotificacaoDia` **não** faz parte do backup:
> é recalculada pelo aplicativo e não deve ser restaurada.

A ordem de inserção na restauração respeita as chaves estrangeiras (pais antes de
filhos), e os **IDs originais são preservados** para manter o histórico íntegro.

## Exportação portátil (JSON)

A exportação "JSON portátil" usa chaves estáveis em inglês e **não** carrega
`checksum` nem envelope. Serve para leitura em outras ferramentas (planilhas,
scripts, BI) e para compatibilidade futura. O conteúdo dos itens é o mesmo do
payload acima.

```
{
  "schemaVersion": 1,
  "exportedAt":  "2026-10-10T12:00:00Z",
  "appVersion":  "0.1.0",
  "projects":     [ ... ],
  "disciplines": [ ... ],
  "topics":       [ ... ],
  "reviews":      [ ... ],
  "preferences": { ... }
}
```

## Validação na restauração

Antes de substituir qualquer dado, o aplicativo verifica, nesta ordem:

1. tamanho do arquivo (≤ 25 MiB) e JSON bem formado;
2. `formato` e `versaoFormato`/`versaoSchemaBanco` (rejeita versões futuras);
3. `checksum` (integridade);
4. conteúdo (semântica): IDs únicos e positivos, FKs apontando para registros
   existentes, campos obrigatórios preenchidos, contagens consistentes, limites
   de tamanho de textos e valores válidos de preferências.

Arquivos inválidos param sem tocar nos dados existentes.

## Exemplo fictício

Backup de um único projeto com uma disciplina e um assunto, em formato `.revisaciclo`
(a formatação "bonita" usa espaços; o `payload` canônico, usado no checksum, não
tem espaços):

```json
{
  "formato": "revisaciclo-backup",
  "versaoFormato": 1,
  "versaoApp": "0.1.0",
  "versaoSchemaBanco": 3,
  "criadoEm": "2026-10-10T10:00:00Z",
  "checksum": {
    "algoritmo": "SHA-256",
    "valor": "0000000000000000000000000000000000000000000000000000000000000000"
  },
  "contagens": {
    "projetos": 1,
    "disciplinas": 1,
    "assuntos": 1,
    "revisoes": 1
  },
  "payload": {
    "projetos": [
      {
        "id": 1,
        "nome": "Concurso Exemplo",
        "descricao": null,
        "cor": 4283215696,
        "criadoEm": 1760090400000,
        "arquivado": false
      }
    ],
    "disciplinas": [
      {
        "id": 2,
        "projetoId": 1,
        "nome": "Matemática",
        "cor": 4280391411,
        "criadaEm": 1760090400000
      }
    ],
    "assuntos": [
      {
        "id": 3,
        "disciplinaId": 2,
        "titulo": "Porcentagem",
        "descricao": "Regra de três e acréscimos.",
        "dataEstudo": "2026-01-05",
        "estadoRevisao": "NOVO",
        "indiceCiclo": 0,
        "proximaRevisao": "2026-01-06"
      }
    ],
    "revisoes": [
      {
        "id": 4,
        "assuntoId": 3,
        "agendadaPara": "2026-01-06",
        "concluidaEm": null,
        "status": "PENDENTE",
        "intervaloDias": 1,
        "indiceCiclo": 0,
        "avaliacao": null
      }
    ],
    "preferencias": {
      "notificacoesAtivas": true,
      "horarioHora": 8,
      "horarioMinuto": 0,
      "metaDiaria": 20,
      "tema": "AUTOMATICO",
      "intervalosDias": [1, 3, 7, 15, 30],
      "projetoAtivoId": 1
    }
  }
}
```

> O `checksum.valor` acima é apenas ilustrativo (zerado) e **não** confere; um
> arquivo assim seria rejeitado na restauração.