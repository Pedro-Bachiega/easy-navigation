#!/usr/bin/env bash
set -euo pipefail

branch="${1:?Branch name is required.}"
version_regex='[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)[0-9]+)?'

if [[ ! "$branch" =~ ^(release|hotfix)/(${version_regex})$ ]]; then
  echo "Merged PRs into main only publish from release/<version> or hotfix/<version> branches."
  echo "Received: ${branch}"
  exit 1
fi

release_type="${branch%%/*}"
version="${branch#*/}"

git fetch --tags --force

if git rev-parse -q --verify "refs/tags/${version}" >/dev/null; then
  echo "Tag ${version} already exists."
  exit 1
fi

latest_stable="$(
  git tag --list |
    grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' |
    sort -V |
    tail -n 1 || true
)"

version_core="${version%%-*}"
IFS='.' read -r major minor patch <<< "$version_core"

if [[ -n "$latest_stable" ]]; then
  IFS='.' read -r latest_major latest_minor latest_patch <<< "$latest_stable"

  if [[ "$release_type" == "hotfix" ]]; then
    if [[ "$major" != "$latest_major" || "$minor" != "$latest_minor" ]]; then
      echo "Hotfix ${version} must keep major/minor from latest stable tag ${latest_stable}."
      exit 1
    fi

    if (( patch <= latest_patch )); then
      echo "Hotfix ${version} must bump the patch above latest stable tag ${latest_stable}."
      exit 1
    fi
  else
    if ! printf '%s\n%s\n' "$latest_stable" "$version_core" | sort -V -C || [[ "$version_core" == "$latest_stable" ]]; then
      echo "Release ${version} must be greater than latest stable tag ${latest_stable}."
      exit 1
    fi
  fi
fi

{
  echo "type=${release_type}"
  echo "version=${version}"
} >> "$GITHUB_OUTPUT"
