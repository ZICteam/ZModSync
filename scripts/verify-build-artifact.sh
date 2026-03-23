#!/usr/bin/env bash
set -euo pipefail

version="${1:-}"
if [[ -z "$version" ]]; then
  version="$(sed -n 's/^mod_version=//p' gradle.properties | head -n 1)"
fi

mod_id="$(sed -n 's/^mod_id=//p' gradle.properties | head -n 1)"

if [[ -z "$version" || -z "$mod_id" ]]; then
  echo "Failed to read mod_id or mod_version from gradle.properties" >&2
  exit 1
fi

artifact="build/libs/${mod_id}-${version}.jar"

if [[ ! -f "$artifact" ]]; then
  echo "Expected build artifact not found: $artifact" >&2
  exit 1
fi

if [[ ! -s "$artifact" ]]; then
  echo "Build artifact exists but is empty: $artifact" >&2
  exit 1
fi

echo "Verified build artifact: $artifact"
