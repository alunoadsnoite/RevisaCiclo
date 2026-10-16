#!/usr/bin/env bash
# Fedora Workstation GNOME x86_64; execute como usuário normal.
set -Eeuo pipefail

GUI=1 MOBILE=1 FLUTTER=1 AGENTS=1 UPDATE=1 DRY_RUN=0
usage() {
  cat <<'HELP'
Uso: bash instalar-fedora-dev.sh [opções]
  --dry-run            Exibe o plano sem alterar ou baixar nada
  --sem-graficos       Não instala VS Code, ChatGPT/Codex desktop, DBeaver ou Bruno
  --sem-android-studio Não baixa o Android Studio (SDK ainda pode ser instalado depois)
  --sem-flutter        Não instala o Flutter
  --sem-agentes        Não instala Hermes, OpenCode e Codex CLI
  --sem-update         Não atualiza os pacotes existentes do Fedora
  --help               Mostra esta ajuda

Instala Git/gh/glab, ferramentas C/C++, Python/uv/pipx, PHP/Composer,
Node LTS via nvm, pnpm, EAS CLI, Java 17/21, Rust/Cargo, Podman/Compose,
clientes PostgreSQL/MariaDB/SQLite, Flutter e ferramentas Android.
Não execute com sudo. O script solicitará sudo apenas para pacotes do sistema.
HELP
}
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --sem-graficos) GUI=0 ;;
    --sem-android-studio) MOBILE=0 ;;
    --sem-flutter) FLUTTER=0 ;;
    --sem-agentes) AGENTS=0 ;;
    --sem-update) UPDATE=0 ;;
    --help|-h) usage; exit 0 ;;
    *) printf 'Opção desconhecida: %s\n' "$arg" >&2; exit 2 ;;
  esac
done
if (( DRY_RUN )); then
  usage
  printf '\nPlano: atualizar=%s gráficos=%s AndroidStudio=%s Flutter=%s agentes=%s\n' \
    "$UPDATE" "$GUI" "$MOBILE" "$FLUTTER" "$AGENTS"
  printf '%s\n' 'Java 17/21: Eclipse Temurin com SHA-256 verificado.' \
    'Android Studio nativo: download oficial com SHA-256 verificado.' \
    'Codex: CLI oficial e aplicativo desktop RPM oficial, se gráficos estiverem habilitados.' \
    'Hermes: instalador oficial, modo não interativo; OpenCode 1: pacote opencode-ai.' \
    'Sem alterações em firewall, SELinux, shell padrão, Git identity ou servidores de banco.'
  exit 0
fi
(( EUID != 0 )) || { echo 'Execute sem sudo: bash instalar-fedora-dev.sh' >&2; exit 1; }
# shellcheck disable=SC1091
source /etc/os-release
[[ "$ID" == fedora ]] || { echo 'Este script é para Fedora Workstation.' >&2; exit 1; }
[[ ! -e /run/ostree-booted ]] || { echo 'Fedora Atomic/Silverblue exige outro procedimento.' >&2; exit 1; }
[[ "$(uname -m)" == x86_64 ]] || { echo 'Este script foi preparado para x86_64.' >&2; exit 1; }
command -v dnf >/dev/null || { echo 'DNF não encontrado.' >&2; exit 1; }

SETUP_DIR="$HOME/.local/state/fedora-dev-setup"
CACHE_DIR="$HOME/.cache/fedora-dev-setup"
DEV_DIR="$HOME/development"
mkdir -p "$SETUP_DIR" "$CACHE_DIR" "$DEV_DIR" "$HOME/.local/bin" "$HOME/Projetos"
LOG="$SETUP_DIR/instalacao-$(date +%Y%m%d-%H%M%S).log"
exec > >(tee -a "$LOG") 2>&1
on_error() {
  local exit_status="$1" line_number="$2"
  printf "\nFalha na linha %s (código %s). Log: %s\nCorrija a causa e execute o script novamente.\n" "$line_number" "$exit_status" "$LOG" >&2
  exit "$exit_status"
}
trap 'on_error "$?" "$LINENO"' ERR
step() { printf '\n>>> %s\n' "$*"; }
download() { curl --proto '=https' --tlsv1.2 --fail --location --retry 3 --connect-timeout 30 --output "$2" "$1"; }
# Downloads só são executados depois de completos; instaladores ficam no cache para inspeção.
sudo -v
if (( UPDATE )); then step 'Atualizando Fedora'; sudo dnf upgrade --refresh -y; fi
step 'Ferramentas de desenvolvimento e clientes de bancos'
sudo dnf install -y \
  git git-lfs gh glab curl wget ca-certificates unzip zip tar xz jq \
  gcc gcc-c++ make clang cmake ninja-build pkgconf-pkg-config \
  autoconf automake libtool patch openssl-devel zlib-devel libffi-devel \
  gtk3-devel mesa-libGLU libatomic libstdc++-devel \
  python3 python3-devel python3-pip pipx uv \
  php-cli php-mbstring php-xml php-pdo php-mysqlnd php-pgsql php-intl php-zip composer \
  rust cargo podman podman-compose buildah skopeo \
  postgresql mariadb sqlite android-tools \
  ripgrep fd-find fzf bat tmux htop tree ShellCheck \
  flatpak fuse fuse-libs bubblewrap chromium \
  gnome-tweaks gnome-extensions-app
# Por repositório, rode git lfs install ao restaurar um projeto que use Git LFS.

step 'Node.js LTS via nvm'
export NVM_DIR="$HOME/.nvm"
if [[ ! -s "$NVM_DIR/nvm.sh" ]]; then
  download 'https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.8/install.sh' "$CACHE_DIR/nvm-install.sh"
  PROFILE=/dev/null bash "$CACHE_DIR/nvm-install.sh"
fi
# nvm não é compatível com nounset em todas as versões.
set +u
# shellcheck disable=SC1091
source "$NVM_DIR/nvm.sh"
nvm install --lts
nvm alias default 'lts/*'
nvm use default
set -u
npm install -g pnpm eas-cli

step 'Java 17 e 21 para builds Android'
for major in 17 21; do
  jdk_dir="$DEV_DIR/jdks/temurin-$major"
  if [[ ! -x "$jdk_dir/bin/javac" ]]; then
    metadata="$CACHE_DIR/temurin-$major.json"
    archive="$CACHE_DIR/temurin-$major.tar.gz"
    download "https://api.adoptium.net/v3/assets/latest/$major/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse" "$metadata"
    url="$(jq -er '.[0].binary.package.link' "$metadata")"
    checksum="$(jq -er '.[0].binary.package.checksum' "$metadata")"
    [[ "$url" == https://github.com/adoptium/temurin*-binaries/releases/download/* ]]
    [[ "$checksum" =~ ^[[:xdigit:]]{64}$ ]]
    download "$url" "$archive"
    printf '%s  %s\n' "$checksum" "$archive" | sha256sum -c -
    mkdir -p "$jdk_dir"
    tar -xzf "$archive" -C "$jdk_dir" --strip-components=1
  fi
done

step 'Configurando PATH para Bash e Zsh'
mkdir -p "$HOME/.config/fedora-dev-setup"
cat > "$HOME/.config/fedora-dev-setup/env.sh" <<'ENV'
# Gerado por instalar-fedora-dev.sh; sem credenciais.
export JDK17_HOME="$HOME/development/jdks/temurin-17"
export JDK21_HOME="$HOME/development/jdks/temurin-21"
export JAVA_HOME="$JDK17_HOME"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export NVM_DIR="$HOME/.nvm"
export PATH="$HOME/.local/bin:$HOME/development/flutter/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$PATH"
if [ -s "$NVM_DIR/nvm.sh" ]; then . "$NVM_DIR/nvm.sh"; fi
ENV
for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do
  touch "$rc"
  # Expansão de HOME ocorrerá ao abrir o terminal, não ao gerar a configuração.
  # shellcheck disable=SC2016
  line='[ ! -f "$HOME/.config/fedora-dev-setup/env.sh" ] || . "$HOME/.config/fedora-dev-setup/env.sh"'
  if ! grep -Fqx "$line" "$rc"; then printf '\n%s\n' "$line" >> "$rc"; fi
done
export JAVA_HOME="$DEV_DIR/jdks/temurin-17"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$HOME/.local/bin:$DEV_DIR/flutter/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

if (( AGENTS )); then
  step 'Codex CLI, OpenCode e Hermes'
  download 'https://chatgpt.com/codex/install.sh' "$CACHE_DIR/codex-install.sh"
  sh "$CACHE_DIR/codex-install.sh"
  npm install -g opencode-ai
  if ! command -v hermes >/dev/null 2>&1; then
    download 'https://hermes-agent.nousresearch.com/install.sh' "$CACHE_DIR/hermes-install.sh"
    bash "$CACHE_DIR/hermes-install.sh" --non-interactive
  else
    echo 'Hermes já está instalado; preservando sua instalação e configuração.'
  fi
fi

if (( FLUTTER )); then
  step 'Flutter stable'
  if [[ ! -d "$DEV_DIR/flutter" ]]; then
    git clone --depth 1 --branch stable https://github.com/flutter/flutter.git "$DEV_DIR/flutter"
  fi
  [[ -x "$DEV_DIR/flutter/bin/flutter" ]] || { echo 'Pasta Flutter existente não contém um SDK válido.'; exit 1; }
  flutter config --enable-linux-desktop --enable-web
  flutter precache --linux --android --web
fi

if (( MOBILE )); then
  step 'Android Studio nativo (download de aproximadamente 1,6 GB)'
  # Versão e hash publicados na página oficial em 15/10/2026.
  studio_dir="$DEV_DIR/android-studio"
  if [[ ! -x "$studio_dir/bin/studio" && ! -x "$studio_dir/bin/studio.sh" ]]; then
    download 'https://edgedl.me.gvt1.com/android/studio/ide-zips/2026.2.1.8/android-studio-rabbit1-linux.tar.gz' "$CACHE_DIR/android-studio.tar.gz"
    printf '%s  %s\n' 'f8775c67cf899d9133712a2d7ee3ef7231e07fb16dbe0544c47e5f5babd5931b' "$CACHE_DIR/android-studio.tar.gz" | sha256sum -c -
    tar -xzf "$CACHE_DIR/android-studio.tar.gz" -C "$DEV_DIR"
  fi
  sudo dnf install -y zlib.i686 ncurses-libs.i686 bzip2-libs.i686
  studio_exec="$studio_dir/bin/studio"
  [[ -x "$studio_exec" ]] || studio_exec="$studio_dir/bin/studio.sh"
  [[ -x "$studio_exec" ]]
  ln -sfn "$studio_exec" "$HOME/.local/bin/android-studio"
  mkdir -p "$HOME/.local/share/applications"
  cat > "$HOME/.local/share/applications/android-studio.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=Android Studio
Exec="$studio_exec" %f
Icon=$studio_dir/bin/studio.svg
Terminal=false
Categories=Development;IDE;
DESKTOP
  if getent group kvm >/dev/null; then
    sudo usermod -aG kvm "$(id -un)"
    echo 'Permissão kvm preparada; encerre a sessão e entre novamente para ativá-la.'
  fi
fi

if (( GUI )); then
  step 'VS Code (repositório oficial Microsoft)'
  sudo rpm --import https://packages.microsoft.com/keys/microsoft.asc
  # Não substitui um arquivo de repositório que já exista.
  if [[ ! -f /etc/yum.repos.d/vscode.repo ]]; then
    sudo tee /etc/yum.repos.d/vscode.repo >/dev/null <<'REPO'
[code]
name=Visual Studio Code
baseurl=https://packages.microsoft.com/yumrepos/vscode
enabled=1
autorefresh=1
type=rpm-md
gpgcheck=1
gpgkey=https://packages.microsoft.com/keys/microsoft.asc
REPO
  fi
  sudo dnf install -y code
  for extension in dbaeumer.vscode-eslint esbenp.prettier-vscode ms-python.python Dart-Code.flutter redhat.java; do
    code --install-extension "$extension" || printf 'Extensão pendente: %s\n' "$extension"
  done
  step 'Aplicativo ChatGPT/Codex para Fedora (RPM oficial)'
  download 'https://persistent.oaistatic.com/codex-app-prod/linux/rpm/latest/chatgpt.x86_64.rpm' "$CACHE_DIR/chatgpt.x86_64.rpm"
  sudo dnf install -y "$CACHE_DIR/chatgpt.x86_64.rpm"
  step 'DBeaver e Bruno via Flatpak'
  flatpak remote-add --user --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo
  flatpak install --user --noninteractive -y flathub io.dbeaver.DBeaverCommunity com.usebruno.Bruno
fi

step 'Resumo'
for tool in git gh glab node npm pnpm python3 uv php composer javac podman; do
  if command -v "$tool" >/dev/null; then printf 'OK: %s (%s)\n' "$tool" "$(command -v "$tool")"; fi
done
if (( AGENTS )); then
  for tool in codex opencode hermes; do command -v "$tool"; done
fi
if (( FLUTTER )); then flutter doctor || echo 'Conclua os itens indicados pelo flutter doctor.'; fi
cat <<'NEXT'

Instalação concluída. Encerre a sessão e entre novamente; abra um terminal novo.

Próximos passos:
1. gh auth login                         # acesso aos seus projetos GitHub
2. glab auth login                       # GitLab/F-Droid
3. codex                                # login com sua conta ChatGPT
4. opencode                             # escolha e autentique o provedor
5. hermes setup                         # configure seu provedor
6. android-studio                       # assistente inicial e Android SDK
   No SDK Manager, instale Android SDK Platform, Build-Tools, Platform-Tools,
   Android SDK Command-line Tools (latest) e Emulator, se for usá-lo.
   Escolha os níveis de API e o NDK exigidos por cada projeto.
7. flutter config --android-sdk "$HOME/Android/Sdk"
   flutter doctor --android-licenses     # leia e aceite as licenças interativamente
   flutter doctor -v

Java 17 é o padrão deste ambiente para seus projetos Expo antigos.
Para um projeto que exige Java 21:
  JAVA_HOME="$JDK21_HOME" PATH="$JDK21_HOME/bin:$PATH" ./gradlew assembleRelease
Node por projeto: use nvm install e nvm use quando houver .nvmrc.
Expo é executado por projeto com npx expo; não há instalação global do expo-cli antigo.
Podman já está disponível; nenhum banco, container ou porta foi iniciado.
Restaure suas chaves privadas a partir do backup do Google Drive antes de assinar APKs.
O Linux não compila aplicativos iOS: essa etapa requer macOS/Xcode ou serviço de build.
NEXT
printf '\nLog: %s\n' "$LOG"
