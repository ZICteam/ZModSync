# Changelog

All notable changes to this project are documented in this file.

The format is intentionally simple:
- Every repository change must bump the mod version.
- Every repository change must add a matching changelog entry.
- Documentation must be updated in the same change whenever behavior, setup, or usage changes.

## [1.0.54] - 2026-03-23

### Changed
- Added a mini sync-pipeline integration test that runs `ManifestGenerator`, `ClientFileScanner`, and `SyncComparator` together on temp server/client fixtures to verify matched, changed, and missing file scenarios.

## [1.0.53] - 2026-03-23

### Changed
- Added direct `ManifestGenerator` coverage for multi-category manifest generation, manifest-copy writing, skipped extensions, relative paths, restart flags, and missing-root handling on temp repository fixtures.

## [1.0.52] - 2026-03-23

### Changed
- Added direct `ClientFileScanner` coverage for category/root scanning, skipped extensions, relative-path generation, restart flags, and missing-root handling using temp filesystem fixtures.

## [1.0.51] - 2026-03-23

### Changed
- Added coverage for client/runtime state holders: `ClientSyncContext`, `SyncIssueState`, and `SyncLogBuffer`, including pre-join readiness consumption, issue-message normalization, and capped log buffering.

## [1.0.50] - 2026-03-23

### Changed
- Added `HashUtils` coverage for known SHA-256 values, empty-file hashing, content-change sensitivity, and lowercase 64-character hex formatting.

## [1.0.49] - 2026-03-23

### Changed
- Expanded `ManagedStateStore` coverage from file-name generation to full save/load behavior, blank-server handling, and graceful fallback on invalid persisted JSON.

## [1.0.48] - 2026-03-23

### Changed
- Added `FileHashCache` coverage for cache persistence, refresh on file changes, scope cleanup, and separation between server/client cache scopes.

## [1.0.47] - 2026-03-23

### Fixed
- Removed the two Forge 47.4.18 compile-time removal warnings by switching `ResourceLocation` creation to `fromNamespaceAndPath(...)` and replacing deprecated `ModLoadingContext.get()` config registration with direct `ModContainer` registration.

## [1.0.46] - 2026-03-23

### Changed
- Updated the Forge build dependency from `47.2.0` to `47.4.18` and rebuilt the mod against that newer Forge 1.20.1 base.

## [1.0.45] - 2026-03-23

### Changed
- Added automatic SHA-256 checksum generation for the built jar, wired it into the shared smoke helper, and included checksum files in CI artifacts and GitHub Releases.

## [1.0.44] - 2026-03-23

### Changed
- Added explicit jar metadata verification for `mods.toml` and `MANIFEST.MF`, and wired it into the shared smoke helper alongside the existing artifact-name check.

## [1.0.43] - 2026-03-23

### Changed
- Added explicit build-artifact verification and wired it into the shared smoke helper so release preparation now checks that the expected versioned jar exists and is non-empty.

## [1.0.42] - 2026-03-23

### Changed
- Added a release-notes rendering helper that extracts the current version section from `CHANGELOG.md` and feeds it into the GitHub Release workflow.

## [1.0.41] - 2026-03-23

### Changed
- Switched both GitHub Actions workflows to the shared `run-release-smoke.sh` helper so local release checks and CI/release validation now use the same execution path.

## [1.0.40] - 2026-03-23

### Changed
- Added a reusable `run-release-smoke.sh` helper that runs tests, verifies a warning-clean build, and optionally checks release-tag/version consistency before publishing.

## [1.0.39] - 2026-03-23

### Changed
- Added a release-version verification script and wired it into the GitHub Release workflow so tag, `gradle.properties`, and the top `CHANGELOG.md` entry must match before publishing.

## [1.0.38] - 2026-03-23

### Changed
- Hardened GitHub Actions with workflow concurrency groups, in-progress cancellation, explicit permissions for the build workflow, and job timeouts for both build and release pipelines.

## [1.0.37] - 2026-03-23

### Changed
- Tightened GitHub Actions so CI now runs `./gradlew test`, verifies `./gradlew build --warning-mode all`, and uploads the HTML test report artifact for successful or failed runs.
- Updated the release workflow to run tests and the warning-clean build before publishing a GitHub Release.

## [1.0.36] - 2026-03-23

### Fixed
- Added an explicit `junit-platform-launcher` test runtime dependency, which removes the Gradle 9 deprecation about automatic test framework implementation loading in the current local build path.

## [1.0.35] - 2026-03-23

### Changed
- Replaced the aggregate JUnit dependency with explicit `junit-jupiter-api` plus `junit-jupiter-engine` as a first cleanup step for Gradle 9 compatibility work.

## [1.0.34] - 2026-03-23

### Changed
- Re-checked the remaining Gradle 9 compatibility concern and confirmed that both `./gradlew build` and `./gradlew build --warning-mode all` currently complete without reproducing the earlier generic deprecation summary.
- Updated the roadmap and documentation to reflect that the warning is not currently reproducible in the local build path, while keeping future Gradle upgrades under observation.

## [1.0.33] - 2026-03-23

### Changed
- Added automated tests for manifest entry identity keys and copy semantics, including protection against accidental mutation of copied entries.

## [1.0.32] - 2026-03-23

### Changed
- Expanded file utility coverage for protected ModSync file detection, covering case-insensitive matching, path-based checks, and rejection of blank or unrelated names.

## [1.0.31] - 2026-03-23

### Changed
- Expanded file utility coverage for relative path normalization and case-insensitive skipped-extension matching used by manifest generation and client scans.

## [1.0.30] - 2026-03-23

### Changed
- Added automated tests for category-to-HTTP segment mapping and default restart flags, covering case-insensitive segment resolution and unknown segment rejection.

## [1.0.29] - 2026-03-23

### Changed
- Added automated tests for server sync status caching, covering refresh decisions, checking-state suppression, stale snapshot expiry, and dirty-state invalidation.

## [1.0.28] - 2026-03-23

### Changed
- Extracted managed-state file naming into a testable helper and added coverage for sanitized server IDs, stable naming, and collision avoidance when different server IDs normalize to the same visible prefix.

## [1.0.27] - 2026-03-23

### Changed
- Added automated tests for restart-state bookkeeping, covering restart flags, prompt consumption, change ordering, and duplicate suppression for repeated entries.

## [1.0.26] - 2026-03-23

### Changed
- Added automated tests for hidden MOTD metadata encoding and decoding, covering round-trip port discovery, replacement of stale metadata, and malformed metadata handling.

## [1.0.25] - 2026-03-23

### Changed
- Extracted manifest URL resolution into a dedicated helper so reverse-proxy and fallback URL rules are testable outside the HTTP handler.
- Added automated tests for `host:port` parsing, IPv6 normalization, configured public base URL trimming, and deterministic fallback candidate ordering.

## [1.0.24] - 2026-03-23

### Changed
- Extracted JSON packet chunking and reassembly into a dedicated helper used by the sync handshake and start-download packet flow.
- Added automated tests for empty payloads, exact chunk boundaries, short tail chunks, multi-chunk reassembly, and accumulator reset behavior.

## [1.0.23] - 2026-03-23

### Changed
- Made manifest comparison expose explicit result types for match, missing file, size change, and hash change instead of keeping all sync-difference logic as a single boolean.
- Expanded automated sync comparison coverage for hash case-insensitivity, missing files, size changes, hash changes, and category-separated identity keys.

## [1.0.22] - 2026-03-23

### Changed
- Extracted sync progress state resolution into a small testable helper instead of keeping all state precedence inside the screen class.
- Added automated tests for the sync progress state order, including failure, active download, restart-required, waiting, and complete states.

## [1.0.21] - 2026-03-22

### Changed
- Investigated the remaining Gradle 9 compatibility warnings and confirmed the explicit test-runtime warning is gone.
- Updated the roadmap to reflect that the remaining generic deprecation summary likely comes from deeper plugin behavior rather than the current build script layer.

## [1.0.20] - 2026-03-22

### Changed
- Added a release guide describing how to cut version tags and publish GitHub Releases with the new workflow.
- Updated the roadmap and docs index to include the release operations document.

## [1.0.19] - 2026-03-22

### Fixed
- Removed the Gradle test-runtime deprecation by declaring the JUnit Jupiter engine explicitly.

## [1.0.18] - 2026-03-22

### Changed
- Modernized parts of the Gradle build script to use more current task and publishing patterns.
- Reduced Gradle build-script technical debt while keeping ForgeGradle behavior unchanged.

## [1.0.17] - 2026-03-22

### Changed
- Added a GitHub Releases workflow that builds the mod on version tags and publishes the jar plus changelog as release assets.
- Updated the roadmap and release documentation for tag-based releases.

## [1.0.16] - 2026-03-22

### Changed
- Added automated tests for download-flow error messaging and friendly exception formatting.
- Expanded the next-step roadmap item for download-flow test coverage.

## [1.0.15] - 2026-03-22

### Changed
- Added automated tests for cleanup decision logic, covering preserved, protected, modified, and deletable file cases.
- Expanded the next-step roadmap item for cleanup/download-related test coverage.

## [1.0.14] - 2026-03-22

### Changed
- Extended CI so successful GitHub Actions builds upload the generated mod jar as an artifact.
- Added a follow-up roadmap section for the next phase after the baseline stabilization work.

## [1.0.13] - 2026-03-22

### Changed
- Added an operations guide for administrators covering release, rollback, and troubleshooting workflows.
- Updated documentation indexes and roadmap status for the admin operations package.

## [1.0.12] - 2026-03-22

### Changed
- Expanded server documentation with practical deployment layouts and reverse proxy examples.
- Added clearer setup guidance for direct HTTP exposure versus proxy-based delivery.

## [1.0.11] - 2026-03-22

### Changed
- Improved the sync progress screen with a clearer status panel for checking, downloading, completion, failure, and restart-required states.
- Disabled leaving the progress screen while downloads are active to make the flow less confusing.

## [1.0.10] - 2026-03-22

### Changed
- Reduced duplicate filesystem metadata reads during manifest generation and client scans, which helps a bit on large modpacks and many-file sync sets.
- Updated roadmap and docs to reflect baseline large-pack performance work.

## [1.0.9] - 2026-03-22

### Changed
- Added minimal JUnit 5 coverage for path safety, file comparison, and manifest JSON round trips.
- Enabled the Gradle `test` task on JUnit Platform and updated roadmap progress for baseline automated tests.

## [1.0.8] - 2026-03-22

### Changed
- Improved repository build ergonomics by standardizing on the Gradle wrapper in documentation.
- Added a GitHub Actions workflow to build the mod on push and pull request.
- Updated roadmap and docs to reflect the repository/CI setup milestone.

## [1.0.7] - 2026-03-22

### Changed
- Improved network and download error reporting with clearer failure messages in both logs and the sync screen.
- Added a visible sync error summary to the progress UI.

## [1.0.6] - 2026-03-22

### Changed
- Added a dedicated smoke checklist for manual verification after important changes.
- Updated roadmap and documentation indexes to include the release verification checklist.

## [1.0.5] - 2026-03-22

### Fixed
- Made cleanup safer by deleting obsolete managed files only when the local file still matches the last ModSync-managed version.
- Prevented cleanup from removing protected ModSync mod files.

### Changed
- Updated roadmap and documentation to reflect completion of the cleanup hardening task.

## [1.0.4] - 2026-03-22

### Fixed
- Simplified the first-join flow so the connect action now completes pre-join sync and then continues into the server automatically.
- Skipped the duplicate post-login handshake when pre-join sync has already finished successfully for the same server.

### Changed
- Split "connect" and "download only" behavior in the multiplayer screen so they no longer run the exact same flow.
- Updated roadmap and documentation to reflect completion of the first-entry flow cleanup task.

## [1.0.3] - 2026-03-22

### Fixed
- Unified download URL generation so `public_http_base_url` is now respected consistently in both the HTTP manifest and the network handshake path.

### Changed
- Updated roadmap and documentation to reflect completion of the first P0 networking consistency task.

## [1.0.2] - 2026-03-22

### Changed
- Added a dedicated roadmap document for the mod.
- Captured the current development priorities with a focus on the core sync workflow instead of mod profiles.
- Updated documentation indexes to include the roadmap and current version.

## [1.0.1] - 2026-03-22

### Changed
- Added a project changelog and release-tracking workflow.
- Defined the rule that each change must include a mod version bump, changelog update, and documentation review.
- Updated the main documentation to point contributors to the changelog and versioning process.
