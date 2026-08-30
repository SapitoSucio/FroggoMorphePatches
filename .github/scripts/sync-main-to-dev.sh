#!/usr/bin/env bash
set -euo pipefail

preserved_paths=(
  "CHANGELOG.md"
  "README.md"
  "gradle.properties"
  "patches-bundle.json"
  "patches-list.json"
)

is_preserved_path() {
  local candidate="$1"

  for path in "${preserved_paths[@]}"; do
    if [[ "$candidate" == "$path" ]]; then
      return 0
    fi
  done

  return 1
}

git fetch origin main dev --tags
git switch --detach origin/dev

if git merge --no-ff --no-commit origin/main -m "chore: Sync main into dev [skip ci]"; then
  :
else
  while IFS= read -r conflict; do
    [[ -z "$conflict" ]] && continue

    if ! is_preserved_path "$conflict"; then
      echo "::error::Cannot sync main into dev because $conflict has a real merge conflict." >&2
      git merge --abort
      exit 1
    fi

    git restore --ours --staged --worktree -- "$conflict"
  done < <(git diff --name-only --diff-filter=U)
fi

if ! git rev-parse --verify MERGE_HEAD >/dev/null 2>&1; then
  echo "main is already synchronized with dev"
  exit 0
fi

# Keep each channel's generated release metadata independent. The merge still
# carries main's source changes and release tag ancestry into dev.
for path in "${preserved_paths[@]}"; do
  if git cat-file -e "HEAD:$path" 2>/dev/null; then
    git restore --source=HEAD --staged --worktree -- "$path"
  else
    git rm -f --ignore-unmatch -- "$path"
  fi
done

git add -A
git commit -m "chore: Sync main into dev [skip ci]"
git push origin HEAD:dev
