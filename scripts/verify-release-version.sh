#!/usr/bin/env bash
set -euo pipefail

tag="${1:-}"
if [[ -z "$tag" ]]; then
  echo "Usage: verify-release-version.sh <tag>" >&2
  exit 1
fi

if [[ "$tag" == refs/tags/* ]]; then
  tag="${tag#refs/tags/}"
fi

if [[ "$tag" != v* ]]; then
  echo "Release tag must start with 'v': $tag" >&2
  exit 1
fi

tag_version="${tag#v}"
gradle_version="$(sed -n 's/^mod_version=//p' gradle.properties | head -n 1)"
changelog_version="$(sed -n 's/^## \[\(.*\)\] - .*$/\1/p' CHANGELOG.md | head -n 1)"

if [[ -z "$gradle_version" ]]; then
  echo "Failed to read mod_version from gradle.properties" >&2
  exit 1
fi

if [[ -z "$changelog_version" ]]; then
  echo "Failed to read top changelog version from CHANGELOG.md" >&2
  exit 1
fi

if [[ "$tag_version" != "$gradle_version" ]]; then
  echo "Tag version '$tag_version' does not match gradle.properties version '$gradle_version'" >&2
  exit 1
fi

if [[ "$tag_version" != "$changelog_version" ]]; then
  echo "Tag version '$tag_version' does not match top CHANGELOG version '$changelog_version'" >&2
  exit 1
fi

echo "Verified release version: tag=$tag_version gradle=$gradle_version changelog=$changelog_version"
