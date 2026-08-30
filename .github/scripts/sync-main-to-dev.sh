#!/usr/bin/env bash
set -euo pipefail

preserved_paths=(
  "CHANGELOG.md"
  "README.md"
  "gradle.properties"
  "patches-bundle.json"
  "patches-list.json"
)

git fetch origin main dev --tags
git switch --detach origin/dev

if ! git merge --no-ff --no-commit -X ours origin/main -m "chore: Sync main into dev [skip ci]"; then
  echo "::error::Cannot sync main into dev because Git could not complete the merge." >&2
  git merge --abort
  exit 1
fi

if ! git rev-parse --verify MERGE_HEAD >/dev/null 2>&1; then
  echo "main is already synchronized with dev"
  exit 0
fi

# Keep each channel's generated release metadata independent. Clean changes from
# main are merged, while conflicting hunks favor dev because it contains the
# active experimental work. The merge still carries main's release tag ancestry.
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
