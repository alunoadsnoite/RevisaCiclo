# RevisaCiclo

Aplicativo Android nativo de **revisão espaçada** para estudantes brasileiros.
Esta primeira entrega contém apenas a **estrutura inicial** do projeto e a tela
**Dashboard**, com dados de demonstração claramente identificados. O algoritmo
completo de revisão espaçada ainda **não** foi implementado.

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
│   └── repository           # implementação do repositório + dados de demonstração
├── domain
│   ├── model                # modelos de domínio
│   └── repository           # interface do repositório
├── ui
│   ├── theme                # tema claro e escuro (Material 3)
│   └── dashboard            # ViewModel + tela do Dashboard
└── navigation               # Navigation Compose
```

### Banco de dados (Room)

- `disciplinas` — disciplinas de estudo.
- `assuntos` — assuntos pertencentes a uma disciplina (chave estrangeira).
- `revisoes` — histórico/agenda de revisões por assunto (chave estrangeira).

## Estado atual

- Estrutura de projeto, tema claro/escuro e navegação básica.
- Banco Room preparado para disciplinas, assuntos e histórico de revisões.
- Tela **Dashboard** em português brasileiro com:
  - progresso circular geral;
  - contadores de disciplinas, assuntos e revisões pendentes;
  - próxima revisão agendada;
  - progresso por disciplina.
- Dados de demonstração inseridos na primeira execução, sempre sinalizados na
  interface com o aviso **"Dados de demonstração"**.
- Testes unitários da lógica de progresso do Dashboard.

## Próximos passos recomendados

1. Modelar e implementar o algoritmo de revisão espaçada (intervalos por ciclo).
2. Criar telas de CRUD de disciplinas e assuntos.
3. Registrar revisões (concluir/adiar) e recalcular a agenda.
4. Notificações/lembretes locais (sem serviços pagos).
5. Migrar a injeção de dependências manual para Hilt, se necessário.

## Licença

Distribuído sob a **GNU General Public License v3.0 ou posterior**
(GPL-3.0-or-later). Consulte o arquivo [LICENSE](LICENSE).
