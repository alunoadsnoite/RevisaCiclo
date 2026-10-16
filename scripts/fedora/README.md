# Ambiente de desenvolvimento — Fedora Workstation GNOME

Preparado em 15/10/2026 para Fedora Workstation x86_64 com DNF (não Silverblue/Atomic).
O aplicativo ChatGPT/Codex preview é oficialmente suportado em Fedora 43/44 na documentação consultada.
Não execute estes scripts no Debian atual: guarde-os e execute após instalar o Fedora.

## Instalar

Baixe `instalar-fedora-dev.sh`, confira o conteúdo e rode como seu usuário:

```bash
bash instalar-fedora-dev.sh --dry-run
bash instalar-fedora-dev.sh
```

O script pede senha de sudo para pacotes de sistema. Instala ferramentas do usuário sem sudo.
Uma conexão estável e vários GB livres são necessários: Android Studio tem aproximadamente 1,6 GB,
Flutter baixa artefatos adicionais, e SDKs/imagens de emuladores ocupam vários GB.
Não reinicia a máquina automaticamente. Encerre a sessão e entre novamente após a instalação.

Para uma instalação menor:

```bash
bash instalar-fedora-dev.sh --sem-android-studio --sem-graficos
```

Opções adicionais: `--sem-flutter`, `--sem-agentes`, `--sem-update`, `--help`.
`--sem-graficos` controla editores/clientes gráficos; Flutter e Android Studio têm opções próprias.
Os instaladores baixados ficam em `~/.cache/fedora-dev-setup`; logs em `~/.local/state/fedora-dev-setup`.
Falhas interrompem a instalação e indicam o log. Depois de resolver a causa, pode executar novamente.
Não trata erros de autenticação automaticamente nem inclui tokens ou senhas.

## Ferramentas incluídas

| Área | Ferramentas |
| --- | --- |
| Editor e Git | VS Code nativo, Git, Git LFS, GitHub CLI, GitLab CLI |
| Frontend/React Native | Node LTS por nvm, npm, pnpm, EAS CLI; Expo via `npx expo` por projeto |
| Backend | Python, uv, pipx, PHP com extensões usuais, Composer, Rust/Cargo |
| Mobile | Android Studio nativo, adb/fastboot, Eclipse Temurin JDK 17 e 21, Flutter/Dart |
| Containers | Podman, Podman Compose, Buildah, Skopeo |
| Bancos e APIs | Clientes PostgreSQL, MariaDB e SQLite; DBeaver e Bruno via Flatpak |
| Agentes | Codex CLI, ChatGPT/Codex desktop RPM oficial, OpenCode 1 (`opencode-ai`), Hermes Agent |
| Linux/GNOME | Compiladores, GTK 3 para Flutter, Chromium, ferramentas de terminal, GNOME Tweaks e Extensions |

Podman é o runtime de containers escolhido; Docker Engine não é instalado. A maioria dos fluxos simples
com Compose pode ser adaptada para `podman-compose`, mas ferramentas que exigem o socket Docker podem
precisar de configuração específica. Nenhum banco/container é criado e nenhum serviço de banco é habilitado.

## Finalizar Android e agentes

Abra Android Studio e conclua o assistente. No SDK Manager instale Android SDK Command-line Tools (latest),
Platform-Tools, Build-Tools e as plataformas/NDKs exigidos pelos projetos. SDK esperado: `~/Android/Sdk`.
Imagens de emuladores são opcionais. Aceleração exige virtualização habilitada no BIOS/UEFI e acesso a `/dev/kvm`.
As bibliotecas i686 são instaladas conforme instruções oficiais do Android Studio para Fedora.

```bash
flutter config --android-sdk "$HOME/Android/Sdk"
flutter doctor --android-licenses
flutter doctor -v
gh auth login
glab auth login
codex
opencode
hermes setup
```

Leia e aceite as licenças Android interativamente. Os agentes e clientes Git exigem seus próprios logins.
O instalador oficial Hermes roda com `--non-interactive`; configuração de provedor ocorre depois.
Não executa comandos dos agentes com permissões irrestritas e não muda as configurações deles.
Para OpenCode 2, use o procedimento próprio da versão 2; não instale as duas versões sobre o mesmo comando.

Java 17 é definido neste ambiente por compatibilidade com os projetos Expo antigos. Java 21 fica disponível:

```bash
JAVA_HOME="$JDK21_HOME" PATH="$JDK21_HOME/bin:$PATH" ./gradlew assembleRelease
```

Cada app deve usar a versão de Node indicada pelo projeto (`.nvmrc`, engines e documentação).
Para projetos Expo antigos que não funcionem com o LTS atual, selecione uma versão compatível, por exemplo
`nvm install 22` e `nvm use 22`. Não apaga nem modifica `.nvmrc` de projetos.
Flutter usa a branch stable no momento da execução; confira a versão fixada pelo projeto antes de compilar
(por exemplo, as versões usadas nos pipelines da Bíblia). Não atualiza uma instalação Flutter já existente.
No Linux não se compila iOS localmente: use macOS/Xcode ou serviço de build como EAS.

## Restaurar seus repositórios

Depois de instalar as ferramentas e autenticar o GitHub:

```bash
bash restaurar-projetos.sh --dry-run
bash restaurar-projetos.sh
```

O segundo script clona os projetos e as branches de backup criadas antes da formatação em `~/Projetos`,
incluindo o primeiro projeto PHP privado. Pastas existentes são preservadas, sem reset ou sobrescrita.
Bíblia Livre Linux e Igreja ficam em cópias separadas. As branches de backup preservam trabalho em andamento:
não significam versões prontas para produção. Não executa npm install, migrações nem builds dos projetos.

Baixe separadamente o arquivo `backup-privado-assinaturas-2026-10-15.tar.gz` do seu Drive
(alunoadsnoite@gmail.com). Extraia em `~/Projetos`; os caminhos internos restauram FocusFeed e a Bíblia.
Ajuste `key.properties` se seu nome de usuário/caminhos mudarem. Nunca faça commit desses arquivos privados.
Esse backup contém as chaves identificadas, não o histórico/configuração dos agentes ou todos os arquivos pessoais.

## Fontes oficiais consultadas

- Codex CLI: https://learn.chatgpt.com/docs/codex/cli
- ChatGPT/Codex desktop Linux: https://learn.chatgpt.com/docs/linux/linux-app
- Hermes: https://hermes-agent.nousresearch.com/docs/getting-started/installation
- OpenCode: https://opencode.ai/docs/
- nvm: https://github.com/nvm-sh/nvm
- VS Code: https://code.visualstudio.com/docs/setup/linux
- Flutter: https://docs.flutter.dev/install/manual
- Android Studio: https://developer.android.com/studio/install e https://developer.android.com/studio
- Temurin API: https://api.adoptium.net/
- uv no Fedora: https://packages.fedoraproject.org/pkgs/uv/uv/

## Validação

Sintaxe Bash, ShellCheck e modos de simulação verificados no Debian atual. A guarda de sistema rejeita
execução real fora do Fedora antes de criar pastas ou instalar pacotes. A instalação completa ainda não
foi executada em uma máquina Fedora; nomes de pacotes, disponibilidade de serviços e downloads podem mudar.
