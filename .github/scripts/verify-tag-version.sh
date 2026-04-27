#!/usr/bin/env bash
set -euo pipefail

version="${1:?Version is required.}"
described_version="$(git describe --tags --exact-match)"

if [[ "$described_version" != "$version" ]]; then
  echo "Expected git describe to resolve ${version}, but got ${described_version}."
  exit 1
fi
