#!/usr/bin/env bash
set -euo pipefail

tag="${1:-}"

if [[ -z "$tag" ]]; then
  echo "Usage: render-release-notes.sh <tag>" >&2
  exit 1
fi

if [[ "$tag" == refs/tags/* ]]; then
  tag="${tag#refs/tags/}"
fi

version="${tag#v}"
output="${2:-build/release-notes.md}"

mkdir -p "$(dirname "$output")"

awk -v version="$version" '
BEGIN {
  capture = 0
}
$0 ~ "^## \\[" version "\\] - " {
  capture = 1
  print "# Release " version
  print ""
  next
}
capture && $0 ~ "^## \\[" {
  exit
}
capture {
  print
}
' CHANGELOG.md > "$output"

if [[ ! -s "$output" ]]; then
  echo "Failed to render release notes for version $version from CHANGELOG.md" >&2
  exit 1
fi

echo "Rendered release notes for $version to $output"
