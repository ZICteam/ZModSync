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

mods_version="$(unzip -p "$artifact" META-INF/mods.toml | sed -n 's/^version=\"\(.*\)\"$/\1/p' | head -n 1)"
manifest_version="$(unzip -p "$artifact" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: \(.*\)$/\1/p' | head -n 1 | tr -d '\r')"

if [[ "$mods_version" != "$version" ]]; then
  echo "mods.toml version '$mods_version' does not match expected version '$version'" >&2
  exit 1
fi

if [[ "$manifest_version" != "$version" ]]; then
  echo "MANIFEST.MF Implementation-Version '$manifest_version' does not match expected version '$version'" >&2
  exit 1
fi

echo "Verified build metadata: mods.toml=$mods_version manifest=$manifest_version"
