#!/usr/bin/env bash
set -euo pipefail

tag="${1:-}"

if [[ ! -x "./gradlew" ]]; then
  chmod +x ./gradlew
fi

if [[ ! -x "./scripts/verify-build-artifact.sh" ]]; then
  chmod +x ./scripts/verify-build-artifact.sh
fi

if [[ ! -x "./scripts/verify-build-metadata.sh" ]]; then
  chmod +x ./scripts/verify-build-metadata.sh
fi

if [[ ! -x "./scripts/generate-artifact-checksums.sh" ]]; then
  chmod +x ./scripts/generate-artifact-checksums.sh
fi

echo "[smoke] Running unit tests"
./gradlew test

echo "[smoke] Verifying warning-clean build"
./gradlew build --warning-mode all

echo "[smoke] Verifying expected build artifact"
./scripts/verify-build-artifact.sh

echo "[smoke] Verifying build metadata inside jar"
./scripts/verify-build-metadata.sh

echo "[smoke] Generating checksum for release artifact"
./scripts/generate-artifact-checksums.sh

if [[ -n "$tag" ]]; then
  echo "[smoke] Verifying release version against tag $tag"
  ./scripts/verify-release-version.sh "$tag"
fi

echo "[smoke] Release smoke checks passed"
