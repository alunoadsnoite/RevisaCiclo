# Submissão ao F-Droid — RevisaCiclo

O RevisaCiclo foi submetido ao repositório oficial do F-Droid por *merge request*
ao repositório [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata), que contém os
metadados de build de cada aplicativo.

- **Merge request:** <https://gitlab.com/fdroid/fdroiddata/-/merge_requests/51985>
- **Estado verificado em 11/10/2026:** fechado **sem merge** (`state: closed`,
  `merged_at: null`), atualizado às 04:47 no horário de Recife. O último pipeline
  está aprovado (`success`), mas isso não significa publicação no catálogo.
- **Publicação:** a página <https://f-droid.org/packages/br.com.revisaciclo/>
  e os metadados `metadata/br.com.revisaciclo.yml` na branch `master` do
  `fdroiddata` retornaram HTTP 404 nesta consulta.
- **Outras submissões:** a busca pública por RevisaCiclo também encontrou o MR
  [51984](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/51984), igualmente
  fechado sem merge. Não foi encontrada submissão aberta nessa busca.
- **Motivo do fechamento:** ainda não confirmado; a API de comentários retornou
  HTTP 401. É necessário ler a discussão no GitLab antes de decidir sobre uma
  reabertura ou nova submissão.

## Pré-requisitos

### Capturas de tela

Quatro capturas reais em português brasileiro estão disponíveis em
`fastlane/metadata/android/pt-BR/images/phoneScreenshots/`, na ordem:
`1.jpeg` (Início), `2.jpeg` (Disciplinas), `3.jpeg` (Revisões) e
`4.jpeg` (Estatísticas). Os arquivos originais enviados foram preservados,
sem edição. Ainda falta uma captura da roda de estágios.

As capturas e descrições em `pt-BR` e `en-US` integram a versão `0.1.1`
(`versionCode: 2`). O F-Droid precisará utilizar a tag `v0.1.1` para importar
esses arquivos; a submissão original de `0.1.0` permanece fechada.

### Código e versão da submissão original

- [x] **Repositório Git público:** <https://github.com/alunoadsnoite/RevisaCiclo>.
- [x] **Tag/release versionada:** `v0.1.0`.
- [x] `versionCode`/`versionName` coerentes com a tag (`1` / `0.1.0`).
- [x] Licença GPL-3.0-or-later já presente (`LICENSE`).

## Metadados — `metadata/br.com.revisaciclo.yml`

Este arquivo vive no repositório `fdroiddata` (não no nosso). O `Summary` fica em
arquivo separado, para permitir tradução (exigência atual do CI do `fdroiddata`).
O exemplo abaixo registra a submissão original de `0.1.0`; para uma nova
submissão da versão com capturas, usar `versionName: 0.1.1`, `versionCode: 2`,
`commit: v0.1.1`, `CurrentVersion: 0.1.1` e `CurrentVersionCode: 2`.

```yaml
Categories:
  - Science & Education
License: GPL-3.0-or-later
AuthorName: Valdenor
SourceCode: https://github.com/alunoadsnoite/RevisaCiclo
IssueTracker: https://github.com/alunoadsnoite/RevisaCiclo/issues
Changelog: https://github.com/alunoadsnoite/RevisaCiclo/releases

AutoName: RevisaCiclo
Description: |
  RevisaCiclo organises your study material into projects, subjects and topics,
  and schedules reviews in a growing cycle (default 1, 3, 7, 15 and 30 days),
  shown on a circular wheel.

  It works fully offline: your data stays only on the device. You can export a
  complete backup (.revisaciclo), export portable JSON, and restore safely.
  No ads, no tracking and no accounts.

  Features:
  * Study projects, subjects and topics
  * Spaced-repetition cycle with configurable intervals
  * Circular review wheel and review sessions
  * Statistics and daily goal
  * Local notifications

RepoType: git
Repo: https://github.com/alunoadsnoite/RevisaCiclo.git

Builds:
  - versionName: 0.1.0
    versionCode: 1
    commit: v0.1.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.1.0
CurrentVersionCode: 1
```

E o resumo traduzível em `metadata/br.com.revisaciclo/en-US/summary.txt`:

```text
Offline circular spaced-repetition study app
```

Observações:

- `gradle: - yes` executa `assembleRelease`. Como não há `signingConfig` de
  release no `app/build.gradle.kts`, a saída é
  `app/build/outputs/apk/release/app-release-unsigned.apk`; o F-Droid assina.
- Não é necessário `sudo`/prebuild: não há dependências nativas nem geração de
  código fora do Gradle (KSP/Room ocorrem no build).
- **Anti-features:** nenhuma prevista (sem `INTERNET`, sem rastreamento, sem
  anúncios). A ausência de criptografia no backup **não** é anti-feature.

## Validação no buildserver

- [x] **Toolchain:** AGP `9.3.1`, Gradle `9.5.0`, Kotlin `2.4.21`, KSP `2.3.12`,
      `compileSdk`/`targetSdk` `37` — o `fdroid build` **passou**, ou seja, o
      buildserver já dispõe das ferramentas necessárias.
- [ ] **Reprodutibilidade:** opcional para aceitação; requisito para o selo
      "reproducible builds". Hoje o build é offline (deps do Google Maven /
      Maven Central), o que ajuda.
- [x] **`allowBackup="true"`** no manifesto: aceitável; apenas informativo.
- [x] **Sem fontes de terceiros embutidas:** somente AndroidX, Material 3,
      Kotlin e Coroutines.

## Checklist

- [x] Publicar o repositório no GitHub.
- [x] Criar a tag `v0.1.0` no commit a ser publicado.
- [x] Confirmar `./gradlew assembleRelease` local gera APK.
- [ ] Testar em dispositivo real (instrumentados) — hoje pendente por falta de
      hardware.
- [x] Fork de `fdroiddata`, adicionar `metadata/br.com.revisaciclo.yml` e o
      `en-US/summary.txt`, e abrir o merge request.
- [x] CI do F-Droid (`fdroid lint`, `fdroid rewritemeta`, `fdroid build` etc.)
      aprovado.
- [ ] Esclarecer o fechamento do MR 51985 e resolver eventuais solicitações dos
      mantenedores antes de retomar a submissão.
- [ ] Confirmar merge e posterior publicação no catálogo oficial.

## O que **não** fazer (regras da fase)

- Não gerar/armazenar chaves privadas no repositório.
- Não configurar assinatura de release sem autorização.
