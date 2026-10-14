# Submissão ao F-Droid — RevisaCiclo

## Atualização em 14/10/2026

- MR oficial: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/51985.
- Estado consultado: aberto, sem merge. O mantenedor pediu para atualizar este MR
  com as otimizações R8, já presentes desde a versão 0.1.2.
- A versão desta atualização é **0.1.3 / versionCode 4**.
- Inclui R8 e remoção de recursos não utilizados, captura da roda de estágios,
  documentação da autoria/licenças dos gráficos e checksum SHA-256 do Gradle.
- Fastlane fornece descrições, resumo e changelogs em `en-US` e `pt-BR`.
  Há cinco capturas em `pt-BR/images/phoneScreenshots`: Início, Disciplinas,
  Revisões, Estatísticas e Estágios. O ícone vem dos recursos Android.
- A receita no fork deve usar o hash completo do commit da tag `v0.1.3`,
  `subdir: app`, `gradle: [yes]`, `AutoUpdateMode: Version` e
  `UpdateCheckMode: Tags`. Manter apenas a versão mais recente antes do merge.

## Falha anterior do CI

O pipeline https://gitlab.com/alunoadsnoite/fdroiddata/-/pipelines/2939594905
compilou a versão 0.1.1 com sucesso, e as verificações de APK, código-fonte,
schema e lint passaram. A falha foi exclusivamente em `fdroid rewritemeta`:
faltava uma linha vazia entre `AutoName` e `RepoType`. A receita nova deve
ser normalizada com `fdroid rewritemeta` antes do envio.

## Assinatura, recursos e validação

A assinatura será feita pelo F-Droid; não há APK upstream assinado de produção
para verificação de reprodutibilidade. A equivalência bit a bit não foi testada.

O APK contém bibliotecas nativas livres do AndroidX (`libandroidx.graphics.path.so`
e `libdatastore_shared_counter.so`). A distribuição permanece em APK universal;
a divisão por ABI não foi adotada para este aplicativo pequeno.

Autoria e licenças dos gráficos: [GRAPHICS_LICENSES.md](GRAPHICS_LICENSES.md).
A nova captura foi conferida em emulador com dados fictícios. Isso não substitui
os testes instrumentados completos ou a validação em aparelho físico.

A versão 0.1.2 passou na validação local de 147 testes unitários, lintRelease e
assembleRelease. A versão 0.1.3 e a receita atualizada exigem nova validação.
CI aprovado não significa merge nem disponibilidade no catálogo. Confirmar
merge e depois publicação em https://f-droid.org/packages/br.com.revisaciclo/.
