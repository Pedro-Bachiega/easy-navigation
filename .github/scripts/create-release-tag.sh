#!/usr/bin/env bash
set -euo pipefail

version="${1:?Version is required.}"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

git tag -a "$version" -m "Release ${version}"
git push origin "refs/tags/${version}"

echo "created=true" >> "$GITHUB_OUTPUT"
