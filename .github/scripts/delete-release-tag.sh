#!/usr/bin/env bash
set +e

version="${1:?Version is required.}"

git tag -d "$version"
git push origin ":refs/tags/${version}"
