# Submissão ao F-Droid — RevisaCiclo

O RevisaCiclo foi submetido ao repositório oficial do F-Droid por *merge request*
ao repositório [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata), que contém os
metadados de build de cada aplicativo.

- **Merge request:** <https://gitlab.com/fdroid/fdroiddata/-/merge_requests/51985>
- **Estado:** aberto, *mergeable*; pipeline de CI **100% verde** (incluindo
  `fdroid lint` e `fdroid build` no buildserver do F-Droid).

## Pré-requisitos

- [x] **Repositório Git público:** <https://github.com/alunoadsnoite/RevisaCiclo>.
- [x] **Tag/release versionada:** `v0.1.0`.
- [x] `versionCode`/`versionName` coerentes com a tag (`1` / `0.1.0`).
- [x] Licença GPL-3.0-or-later já presente (`LICENSE`).

## Metadados — `metadata/br.com.revisaciclo.yml`

Este arquivo vive no repositório `fdroiddata` (não no nosso). O `Summary` fica em
arquivo separado, para permitir tradução (exigência atual do CI do `fdroiddata`).

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
- [ ] Aguardar o *review*/merge por um mantenedor do `fdroiddata`.

## O que **não** fazer (regras da fase)

- Não gerar/armazenar chaves privadas no repositório.
- Não configurar assinatura de release sem autorização.
