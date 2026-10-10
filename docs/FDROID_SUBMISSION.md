# Submissão ao F-Droid — RevisaCiclo

Rascunho do que falta para publicar o RevisaCiclo no repositório oficial do
F-Droid. **Nada aqui foi submetido.** A submissão é feita por *merge request* ao
repositório [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata), que contém os
metadados de build de cada aplicativo.

## Pré-requisitos (bloqueadores atuais)

- [x] **Repositório Git público:** <https://github.com/alunoadsnoite/RevisaCiclo>.
- [x] **Tag/release versionada:** `v0.1.0`.
- [x] `versionCode`/`versionName` coerentes com a tag (`1` / `0.1.0`).
- [x] Licença GPL-3.0-or-later já presente (`LICENSE`).

## Metadados (rascunho) — `metadata/br.com.revisaciclo.yml`

Este arquivo vai no repositório `fdroiddata` (não no nosso). Confirmar
`AuthorName` e a tag `commit:`.

```yaml
Categories:
  - Science & Education
License: GPL-3.0-or-later
AuthorName: Valdenor
WebSite: https://github.com/alunoadsnoite/RevisaCiclo
SourceCode: https://github.com/alunoadsnoite/RevisaCiclo
IssueTracker: https://github.com/alunoadsnoite/RevisaCiclo/issues
Changelog: https://github.com/alunoadsnoite/RevisaCiclo/releases

AutoName: RevisaCiclo
Summary: Revisão espaçada circular, offline
Description: |
  O RevisaCiclo organiza seus estudos em projetos, disciplinas e assuntos e agenda
  revisões em um ciclo crescente (1, 3, 7, 15, 30 dias por padrão), exibido em uma
  roda circular.

  Funciona totalmente offline: os dados ficam apenas no aparelho. Permite
  exportar um backup completo (.revisaciclo), exportar JSON e restaurar com
  segurança. Sem anúncios, sem telemetria e sem contas.
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

Observações:

- `gradle: - yes` executa `assembleRelease`. Como não há `signingConfig` de
  release no `app/build.gradle.kts`, a saída é
  `app/build/outputs/apk/release/app-release-unsigned.apk`; o F-Droid assina.
- Não é necessário `sudo`/prebuild: não há dependências nativas nem geração de
  código fora do Gradle (KSP/Room ocorrem no build).
- **Anti-features:** nenhuma prevista (sem `INTERNET`, sem rastreamento, sem
  anúncios). A ausência de criptografia no backup **não** é anti-feature.

## Riscos técnicos a verificar no buildserver

- [ ] **Toolchain muito recente:** AGP `9.3.1`, Gradle `9.5.0`, Kotlin `2.4.21`,
      KSP `2.3.12`, `compileSdk`/`targetSdk` `37`. O buildserver do F-Droid pode
      ainda não ter as build-tools/SDK 37 nem o JDK exigido (Java 17 alvo). Se
      falhar, pode ser necessário ajustar versões ou abrir issue no `fdroiddata`.
- [ ] **Reprodutibilidade:** opcional para aceitação; requisito para o selo
      "reproducible builds". Hoje o build é offline (deps do Google Maven /
      Maven Central), o que ajuda.
- [ ] **`allowBackup="true"`** no manifesto: aceitável; apenas informar.
- [ ] **Sem fontes de terceiros embutidas:** somente AndroidX, Material 3,
      Kotlin e Coroutines.

## Checklist antes do MR

- [x] Publicar o repositório no GitHub/GitLab.
- [x] Criar a tag `v0.1.0` no commit a ser publicado.
- [x] Confirmar `./gradlew assembleRelease` local gera APK.
- [ ] Testar em dispositivo real (instrumentados) — hoje pendente por falta de
      hardware.
- [ ] Fork de `fdroiddata`, adicionar `metadata/br.com.revisaciclo.yml`, rodar
      `fdroid lint br.com.revisaciclo` e abrir o merge request.
- [ ] Acompanhar o build do buildserver (`fdroid build br.com.revisaciclo`).

## O que **não** fazer (regras da fase)

- Não publicar nem abrir MR sem autorização.
- Não gerar/armazenar chaves privadas no repositório.
- Não configurar assinatura de release sem autorização.
