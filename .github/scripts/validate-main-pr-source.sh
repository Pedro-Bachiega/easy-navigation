#!/usr/bin/env bash
set -euo pipefail

branch="${1:?Branch name is required.}"
version_regex='[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)[0-9]+)?'

if [[ ! "$branch" =~ ^(release|hotfix)/${version_regex}$ ]]; then
  echo "PRs into main must come from release/<version> or hotfix/<version> branches."
  echo "Received: ${branch}"
  exit 1
fi
