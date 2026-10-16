#!/usr/bin/env bash
# Recupera as branches preservadas antes da formatação, sem sobrescrever pastas.
set -Eeuo pipefail
if [[ "${1:-}" == --help ]]; then
  echo 'Uso: bash restaurar-projetos.sh [--dry-run] [pasta-destino]'; exit 0
fi
DRY_RUN=0
if [[ "${1:-}" == --dry-run ]]; then DRY_RUN=1; shift; fi
DEST="${1:-$HOME/Projetos}"
repos=(
  'RevisaCiclo|RevisaCiclo|backup/antes-formatacao-2026-10-15'
  'tea-mvp|tea-mvp|backup/antes-formatacao-2026-10-15'
  'tea-mvp-childs|tea-mvp-childs|backup/antes-formatacao-2026-10-15'
  'FocusFeed|FocusFeed|main'
  'PixelRicardo|PixelRicardo|main'
  'bibliaappipm|bibliaappipm|backup/antes-formatacao-2026-10-15'
  'bibliaappipm|biblia-igreja|igreja'
  'bibliaappipm|bibliaappipm-copia-gh|backup/copia-gh-antes-formatacao-2026-10-15'
  'biblia-livre-ipm|biblia-livre-ipm|main'
  'biblia-livre-ipm|biblia-livre-linux|codex/linux-appimage'
  'primeiro-projeto-php|php/primeiro-projeto|main'
)
if (( ! DRY_RUN )); then
  command -v gh >/dev/null || { echo 'Instale GitHub CLI (gh) primeiro.'; exit 1; }
  gh auth status || { echo 'Execute gh auth login primeiro.'; exit 1; }
  mkdir -p "$DEST"
fi
for entry in "${repos[@]}"; do
  IFS='|' read -r repo folder branch <<< "$entry"
  if (( DRY_RUN )); then
    printf 'Clonar alunoadsnoite/%s, branch %s, em %s\n' "$repo" "$branch" "$DEST/$folder"
  elif [[ -e "$DEST/$folder" ]]; then
    printf 'Preservando pasta existente: %s\n' "$DEST/$folder"
  else
    mkdir -p "$(dirname "$DEST/$folder")"
    gh repo clone "alunoadsnoite/$repo" "$DEST/$folder" -- --branch "$branch"
  fi
done
printf '\nAs chaves de assinatura são restauradas separadamente do backup privado do Drive.\n'
