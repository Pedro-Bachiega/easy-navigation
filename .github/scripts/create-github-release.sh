#!/usr/bin/env bash
set -euo pipefail

version="${1:?Version is required.}"
notes_file="$(mktemp)"
target_commit="$(git rev-parse HEAD)"

{
  echo "${RELEASE_TITLE:-$version}"
  echo
  jq -r '.pull_request.body // ""' "$GITHUB_EVENT_PATH"
} > "$notes_file"

gh release create "$version" \
  --target "$target_commit" \
  --title "$version" \
  --notes-file "$notes_file"
