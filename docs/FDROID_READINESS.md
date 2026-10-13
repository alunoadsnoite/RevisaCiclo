# Prontidão para F-Droid

Este documento reúne as informações necessárias para distribuir o RevisaCiclo pelo
F-Droid e servem de checklist de conformidade com o software livre.

## Resumo

- **Licença:** GPL-3.0-or-later (arquivo [LICENSE](../LICENSE) na raiz).
- **Código-fonte:** completo e compilável por terceiros; sem dependências
  proprietárias.
- **Funciona 100% offline:** sem conta, login, servidor, anúncios ou telemetria.
- **Sem rastreamento:** nenhum SDK de análise, crash reporting ou publicidade.

## Permissões

Declaradas em `app/src/main/AndroidManifest.xml`:

| Permissão | Motivo |
|---|---|
| `POST_NOTIFICATIONS` | exibir lembretes locais de revisão (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | reagendar os lembretes após reiniciar o aparelho. |

O aplicativo **não** declara `INTERNET`, nem permissões amplas de armazenamento
(`READ/WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`). A exportação e a
restauração usam o Storage Access Framework (SAF), em que o próprio usuário escolhe
o arquivo, sem conceder acesso ao restante do armazenamento.

### Permissões adicionadas na mesclagem do manifesto

A biblioteca AndroidX WorkManager (usada apenas para reagendar lembretes locais)
adiciona permissões ao manifesto final do APK. Elas não são declaradas no código do
aplicativo e não ampliam o acesso a dados do usuário:

| Permissão | Origem | Motivo |
|---|---|---|
| `WAKE_LOCK` | WorkManager | manter a CPU ativa o suficiente para executar o trabalho periódico de reagendamento. |
| `ACCESS_NETWORK_STATE` | WorkManager | apenas observar o estado de conectividade para avaliar restrições de trabalho. O aplicativo não faz nenhuma chamada de rede. |
| `FOREGROUND_SERVICE` | WorkManager | executar trabalhos de longa duração como serviço em primeiro plano, quando aplicável. |
| `br.com.revisaciclo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | permissão de assinatura **interna do próprio app**, usada para registrar receptores dinâmicos não exportados. Não é concedida a terceiros. |

O aplicativo desativa o backup automático do sistema (`android:allowBackup="false"`)
e restringe a transferência entre dispositivos via `android:dataExtractionRules`
(ver [RESTORE_SAFETY.md](RESTORE_SAFETY.md)).

Para **compartilhar** um backup gerado, o aplicativo copia o arquivo para a própria
área de cache (`cacheDir/shared`) e o expõe via `FileProvider`
(`${applicationId}.fileprovider`, `exported=false`, `grantUriPermissions=true`),
concedendo leitura apenas à URI compartilhada (`FLAG_GRANT_READ_URI_PERMISSION`).
Isso não requer permissão de armazenamento e não expõe caminhos privados.

## Dependências

Todas são software livre, publicadas no Google Maven / Maven Central:
AndroidX (Core, Lifecycle, Activity, Compose, Navigation, Room, DataStore, Work),
Material 3, Kotlin stdlib e Coroutines (AndroidX = Apache-2.0; Kotlin = Apache-2.0).
Não há bibliotecas de rastreamento, ads ou crash analytics.

## Anti-recursos

Nenhum: sem anúncios, sem compras no aplicativo, sem dependência de serviços de
jogos, sem rastreamento de usuários, sem exigência de conta.

## Rede e privacidade

- Nenhuma chamada de rede.
- Os dados ficam apenas no dispositivo (Room + DataStore).
- Backups são arquivos locais escolhidos pelo usuário; não são enviados a lugar
  nenhum automaticamente.

## Build

- Compilação reprodutível via Gradle Wrapper versionado (`gradlew`, Gradle 9.5.0).
- JDK 21, Android SDK compileSdk/targetSdk 37, minSdk 26.
- Sem `google-services.json`, sem chaves de API embutidas.
- Assinatura de release **não** é versionada; o F-Droid assina com a própria chave.

Comando de build (a partir do código-fonte):

```bash
./gradlew assembleDebug
```

## Versionamento

- `versionCode` e `versionName` em `app/build.gradle.kts` (atual: `3` / `0.1.2`).
- Releases usam R8 para otimizar e reduzir o código e remover recursos não utilizados.
- A cada release, incremente ambos e crie a tag correspondente no repositório.

## Metadados sugeridos para o F-Droid

| Campo | Valor |
|---|---|
| Nome | RevisaCiclo |
| Categoria | Educação |
| Licença | GPL-3.0-or-later |
| Resumo | Revisão espaçada offline para estudos. |
| Recursos | Offline, Sem anúncios, Sem rastreamento, Software livre. |

## Itens a conferir antes de publicar

- [ ] Tag de release criada e apontando para o commit exato do `versionCode`.
- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug` sem erros.
- [ ] Nenhuma permissão nova sem justificativa.
- [ ] Revisão de dependências (sem novas bibliotecas proprietárias).
- [ ] Textos de loja e capturas de tela em português brasileiro.
