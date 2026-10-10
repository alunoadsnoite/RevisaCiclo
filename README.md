# RevisaCiclo

Aplicativo Android nativo de **revisão espaçada** para estudantes brasileiros.

O aplicativo já conta com o motor de revisão espaçada por ciclos, cadastro de
disciplinas, cadastro de assuntos com data de estudo, a conclusão/avaliação das
revisões pendentes, a **roda circular de estágios**, a **sessão de revisão** em
sequência, a aba de **estatísticas** e **projetos de estudo** independentes. Também
oferece **backup completo** (`.revisaciclo`), **exportação JSON** portátil e
**restauração segura** com recuperação de falhas. Não há dados de demonstração: o
usuário começa com a base vazia e cria o próprio conteúdo.

## Stack

| Componente | Versão |
|---|---|
| Linguagem | Kotlin 2.4.21 (Kotlin embutido no AGP 9) |
| Android Gradle Plugin | 9.3.1 |
| Gradle (Wrapper) | 9.5.0 |
| JDK | 21 (mínimo exigido pelo AGP: 17) |
| Jetpack Compose | BOM 2026.09.00 + Material 3 |
| Room | 2.8.5 (via KSP 2.3.12) |
| Navigation Compose | 2.10.2 |
| Lifecycle / ViewModel | 2.11.0 |
| Coroutines | 1.11.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| applicationId | br.com.revisaciclo |
| Licença | GPL-3.0-or-later |

Arquitetura **MVVM** com `ViewModel`, `StateFlow` e Coroutines, organizada em
`data`, `domain`, `ui` e `navigation`.

## Requisitos de ambiente

1. **JDK 17 ou superior** (testado com JDK 21).
2. **Android SDK** com os pacotes:
   - `platforms;android-37.0`
   - `build-tools;36.0.0`
   - `platform-tools`

Configure a localização do SDK de uma das formas:

- Arquivo `local.properties` na raiz do projeto:
  ```
  sdk.dir=/caminho/para/Android/Sdk
  ```
- Variável de ambiente `ANDROID_HOME`.

> `local.properties` é específico da máquina e não deve ser versionado.

### Instalando os pacotes do SDK (sem Android Studio)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
# baixe e extraia as command-line tools, renomeando a pasta para "latest"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

## Compilação, execução e testes

Todo o build usa o **Gradle Wrapper** do projeto (nunca um Gradle global).

```bash
# Compilar o APK de debug
./gradlew assembleDebug

# Instalar em um dispositivo/emulador conectado
./gradlew installDebug

# Executar os testes unitários (JVM)
./gradlew testDebugUnitTest

# Executar os testes instrumentados (requer dispositivo/emulador)
./gradlew connectedDebugAndroidTest

# Análise estática
./gradlew lint
```

O APK de debug é gerado em:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Estrutura de pacotes

```
br.com.revisaciclo
├── AppContainer.kt          # injeção de dependências manual
├── RevisaCicloApp.kt        # Application
├── MainActivity.kt
├── data
│   ├── local                # Room: banco, entidades, DAOs, mapeadores
│   │   ├── dao
│   │   ├── entity
│   │   └── model            # projeções de consulta
│   ├── preferencias         # DataStore (preferências e DataStore de operação)
│   ├── backup               # backup/restauração: Room, SAF e snapshot
│   └── repository           # implementação do repositório (OfflineRevisaoRepository)
├── domain
│   ├── model                # modelos e enums de domínio
│   ├── repository           # interface do repositório
│   ├── scheduler            # MotorRevisaoEspacada (ciclos e intervalos)
│   ├── agregacao            # cálculos da roda e das estatísticas (puros e testáveis)
│   ├── backup               # formato, serialização, validação e serviços de backup
│   ├── projetos             # seleção e observação do projeto ativo
│   └── sessao               # SessaoDeRevisao (fila de revisões devidas)
├── ui
│   ├── theme                # tema claro e escuro (Material 3)
│   ├── common               # formatação de data, seleção de cor e avaliação
│   ├── dashboard            # ViewModel + tela do Dashboard
│   ├── disciplinas          # CRUD de disciplinas
│   ├── assuntos             # CRUD de assuntos por disciplina
│   ├── revisoes             # revisões pendentes e avaliação
│   ├── roda                 # roda circular de estágios
│   ├── sessao               # sessão de revisão em sequência
│   ├── configuracoes        # preferências e backup/restauração
│   └── estatisticas         # resumo e evolução diária
└── navigation               # Navigation Compose (barra inferior + rotas)
```

### Banco de dados (Room)

Versão atual do schema: **3** (schemas exportados em `app/schemas/`). A migração
`1 → 2` converte as datas de milissegundos para *epoch day* e adiciona os campos
de agendamento; a migração `2 → 3` cria a tabela `projetos_estudo` e associa cada
disciplina a um projeto. Nenhuma migração apaga dados.

- `projetos_estudo` — projetos (concurso, faculdade, estudos pessoais).
- `disciplinas` — disciplinas de um projeto.
- `assuntos` — assuntos de uma disciplina, com data de estudo, estado e próxima revisão.
- `revisoes` — revisões agendadas/concluídas por assunto, com avaliação.

## Motor de revisão espaçada

O `MotorRevisaoEspacada` usa os intervalos `[1, 3, 7, 15, 30]` dias. A avaliação
de uma revisão define o próximo ciclo:

| Avaliação | Próximo ciclo |
|---|---|
| Esqueci | reinicia no ciclo 1 |
| Difícil | repete o ciclo atual |
| Bom | avança 1 ciclo |
| Fácil | avança 2 ciclos |

Após o último intervalo o assunto continua sendo revisado a cada 30 dias.
A próxima data é calculada a partir da data de estudo (agendamento inicial) ou da
data em que a revisão foi avaliada.

## Roda de estágios

A guia **Estágios** apresenta a roda circular com os cinco estágios
(1, 3, 7, 15 e 30 dias), cada um colorido conforme a situação:

- **Vermelho/Atrasada** — revisão vencida;
- **Azul/Terciário/Vence hoje** — revisão do dia;
- **Primário** — demais assuntos dentro do prazo.

Toque num segmento do anel ou numa linha da legenda para ver os assuntos do
estágio (com a data da próxima revisão e a situação). A legenda e a descrição de
acessibilidade reproduzem o conteúdo do desenho para leitores de tela.

## Sessão de revisão

O botão **Iniciar sessão** (em Estágios ou Revisões) abre uma sessão com fila de
revisões devidas (atrasadas ou agendadas para hoje). Cada item é avaliado uma
única vez (Esqueci, Difícil, Bom ou Fácil) e, ao final, um resumo mostra a
quantidade de avaliações de cada tipo. A avaliação é persistida antes de avançar;
se a gravação falhar, o item permanece na fila e a tentativa pode ser repetida.

## Backup, exportação e restauração

Em **Configurações → Backup e dados**:

- **Exportar** gera um **backup completo** (`.revisaciclo`) restaurável pelo
  aplicativo ou um **JSON portátil** para uso em outras ferramentas.
- **Restaurar** lê um `.revisaciclo`, valida a integridade (SHA-256) e o conteúdo,
  mostra uma prévia e, após confirmação, **substitui todos os dados** de forma
  atômica, com snapshot de segurança e recuperação automática em caso de
  interrupção.

Detalhes do formato em [docs/BACKUP_FORMAT.md](docs/BACKUP_FORMAT.md) e do
protocolo em [docs/RESTORE_SAFETY.md](docs/RESTORE_SAFETY.md). Informações para
distribuição livre em [docs/FDROID_READINESS.md](docs/FDROID_READINESS.md).

## Estado atual

- Estrutura de projeto, tema claro/escuro e navegação com barra inferior
  (Início, **Estágios**, Disciplinas, Revisões e **Estatísticas**).
- Banco Room com migração e motor de revisão espaçada (`domain/scheduler`).
- Telas em português brasileiro:
  - **Dashboard**: progresso geral, contadores, próxima revisão e progresso por
    disciplina;
  - **Estágios**: roda circular dos 5 estágios com toque/legenda e descrição
    acessível;
  - **Disciplinas**: criar, editar e excluir;
  - **Assuntos**: criar, editar e excluir, com data de estudo;
  - **Revisões**: lista de revisões pendentes/atrasadas e avaliação (Esqueci,
    Difícil, Bom, Fácil);
  - **Sessão de revisão**: fila de devidas com resumo final;
  - **Configurações**: notificações, meta diária, tema, intervalos do ciclo e
    backup/restauração;
  - **Estatísticas**: revisões concluídas, pendentes e atrasadas + evolução
    diária em 7 e 30 dias.
- Projetos de estudo independentes, com projeto ativo e arquivamento.
- Backup completo (`.revisaciclo`), exportação JSON portátil e restauração segura.
- Sem dados de demonstração (base inicia vazia).
- Testes unitários do Dashboard, do motor de revisão espaçada, da roda
  (agregação e geometria do toque), das estatísticas, da sessão de revisão e do
  backup (JSON, serialização, validação e reconciliação).
- Teste de interface Compose (`androidTest`) para a roda de estágios
  (executável com `connectedDebugAndroidTest`).

## Próximos passos recomendados

1. Filtros e ordenação adicionais na lista de assuntos/revisões.
2. Migrar a injeção de dependências manual para Hilt, se necessário.
3. Notificações por projeto ativo (hoje o lembrete é global).

## Licença

Distribuído sob a **GNU General Public License v3.0 ou posterior**
(GPL-3.0-or-later). Consulte o arquivo [LICENSE](LICENSE).
