# Changelog

All notable changes to this project are documented in this file.

The format is intentionally simple:
- Every repository change must bump the mod version.
- Every repository change must add a matching changelog entry.
- Documentation must be updated in the same change whenever behavior, setup, or usage changes.

## [1.0.91] - 2026-03-26

### Changed
- Rebranded the public project name from `ModSync` to `SyncBridge` for distribution pages and user-facing metadata while keeping the technical `modsync` mod id for compatibility with existing installs and sync flows.
- Replaced the old `MIT` placeholder with `Custom License` in project metadata and added the corresponding repository license file for personal-use-friendly, commercial-use-restricted distribution.

## [1.0.89] - 2026-03-25

### Changed
- Fixed a race in dedicated-server handshake tracking: if the client `hello` reached the server before pending-handshake registration, the player could still be marked pending and then be kicked about 30 seconds later even though ModSync was present and the player was already visible in the world.

## [1.0.88] - 2026-03-25

### Changed
- Added staged self-update support for the client `modsync` jar: when ModSync itself is synchronized as a managed mod, it now downloads into a protected staging area and launches a small post-exit updater so the new jar replaces the old one after Minecraft fully closes.

## [1.0.87] - 2026-03-25

### Changed
- Fixed the dedicated-server post-login flow after successful pre-join sync: the client now still sends a lightweight ModSync handshake acknowledgement so the server does not time out and kick an already synchronized player 30 seconds after login.

## [1.0.86] - 2026-03-25

### Changed
- Added download URL fallback on the client: when a manifest points to an unreachable `public_http_base_url`, ModSync now retries the same file against the currently selected server host and discovered hidden HTTP port before failing the download batch.

## [1.0.85] - 2026-03-25

### Changed
- Stopped requiring the ModSync post-login handshake on integrated/local server sessions, which prevents false "install Forge and ModSync" disconnects when the client correctly skips the multiplayer-only handshake path.
- Increased the multiplayer handshake timeout and replaced the kick text with a ModSync-specific timeout/update message so slow client startup is less likely to be misreported as a missing Forge install.
- Increased default HTTP manifest and file-download timeouts to make large remote modpacks more tolerant of slow or overloaded file hosts during pre-join sync.

## [1.0.84] - 2026-03-23

### Changed
- Hardened `SyncComparator` so malformed local or server entries without usable category, relative path, or SHA-256 are filtered out before comparison instead of reaching `getIdentityKey()` and comparison paths.

## [1.0.83] - 2026-03-23

### Changed
- Hardened `ManifestData` itself so `entries` now normalizes to an empty list and copies assigned lists defensively, reducing the chance of `null` propagation or accidental external mutation across manifest, packet, and sync paths.

## [1.0.82] - 2026-03-23

### Changed
- Hardened the `serverSuggested` fallback path so client download planning now ignores entries without usable download metadata such as missing `downloadUrl` or `sha256`, instead of passing malformed records into the download layer.

## [1.0.81] - 2026-03-23

### Changed
- Expanded manifest/entry normalization so payload records without SHA-256 values are filtered out during JSON parsing, preventing malformed data from reaching sync comparison and download verification paths.

## [1.0.80] - 2026-03-23

### Changed
- Expanded manifest/entry normalization so malformed records without a category or usable relative path are dropped during JSON parsing before they can trigger runtime `NullPointerException` paths in sync comparison, cleanup, or packet planning.

## [1.0.79] - 2026-03-23

### Changed
- Hardened JSON entry parsing so `null` elements inside manifest payloads and entry arrays are filtered out before the data reaches sync comparison, packet planning, or cleanup logic.

## [1.0.78] - 2026-03-23

### Changed
- Hardened manifest JSON parsing so `null` manifests and `entries: null` payloads are normalized to empty `ManifestData` objects instead of propagating `null` into later sync stages.

## [1.0.77] - 2026-03-23

### Changed
- Hardened chunk reassembly so packet payloads now reject invalid chunk order and mismatched chunk-count sequences instead of concatenating inconsistent data, and added direct coverage for those cases.

## [1.0.76] - 2026-03-23

### Changed
- Expanded chunked packet-handshake integration coverage with the stale `serverSuggested` case, verifying that once a full manifest has arrived and matches the client, the client-side decision path ignores stale download suggestions and stays in the no-download state.

## [1.0.75] - 2026-03-23

### Changed
- Expanded the mini packet-handshake integration coverage with the empty-manifest-cache scenario, verifying that the server responds with an empty outbound manifest and the client stays in the no-download path instead of triggering an unnecessary sync.

## [1.0.74] - 2026-03-23

### Changed
- Added a chunked packet-handshake integration test that sends long encoded file paths through JSON serialization, chunk splitting, reassembly, and the client-side start-download planner to verify that long names with spaces and URL-sensitive characters survive the full payload path.

## [1.0.73] - 2026-03-23

### Changed
- Added a mini packet-handshake integration test that runs the server response planner and the client `start-download` planner together, covering both the encoded-filename download case and the fully synchronized no-download case.

## [1.0.72] - 2026-03-23

### Changed
- Added direct coverage for the packet-handshake fallback when no cached server manifest is available yet, including normalization to an empty manifest with a fresh timestamp before the server computes the outbound response.

## [1.0.71] - 2026-03-23

### Changed
- Added direct server-side packet-handshake response coverage for the `client file list` path, including the combined rule that outgoing manifest entries must get encoded download URLs before the required-download set is computed and sent back to the client.

## [1.0.70] - 2026-03-23

### Changed
- Added direct `start-download` decision coverage for the packet handshake path, including the already-synchronized early-exit case, the manifest-based download case, and the fallback behavior when only the server-suggested list is available.

## [1.0.69] - 2026-03-23

### Changed
- Added direct packet-handshake coverage for `NetworkHandler`, including encoded download URLs in the server manifest payload and the rule that `start-download` should prefer manifest-based comparison over stale server-suggested lists when a full manifest is already cached.

## [1.0.68] - 2026-03-23

### Changed
- Added direct embedded file-server coverage for encoded download paths, including approved-entry resolution for file names with spaces and URL-sensitive characters plus rejection of malformed or traversal-style requests.

## [1.0.67] - 2026-03-23

### Changed
- Fixed file download URLs for names with spaces and other URL-sensitive characters by percent-encoding relative paths in manifests/handshake payloads and decoding them safely in the embedded HTTP file server.

## [1.0.66] - 2026-03-23

### Changed
- Added direct post-login orchestration coverage for `ClientBootstrap`, including singleplayer/local-session skipping, duplicate-handshake suppression after successful pre-join sync, and the normal multiplayer handshake path.

## [1.0.65] - 2026-03-23

### Changed
- Added direct pre-join orchestration coverage for `PreJoinSyncManager`, including the decision path for already-synchronized clients, download-required cases, and whether auto-connect should continue immediately or only after downloads complete.

## [1.0.64] - 2026-03-23

### Changed
- Expanded `ServerSyncStatusCache` coverage for local-scan cache reuse, expiry-based rescans, and `markDirty()` invalidation of cached local entries in addition to status snapshots.

## [1.0.63] - 2026-03-23

### Changed
- Added manifest-fetch coverage for the full `serverAddress + discoveredPort + configured HTTP port` path, including discovered-port priority and fallback to the configured ModSync HTTP port.

## [1.0.62] - 2026-03-23

### Changed
- Added direct public-manifest coverage for `ServerManifestHttpHandler`, including download URL generation per category and the empty fallback case when no cached manifest is available.

## [1.0.61] - 2026-03-23

### Changed
- Added local-HTTP manifest-fetch coverage for `ServerManifestHttpHandler`, including successful manifest parsing, fallback to a later candidate URL, and total-failure reporting with attempted URLs.

## [1.0.60] - 2026-03-23

### Changed
- Expanded `DownloadManager` local-HTTP integration coverage with a successful-HTTP but invalid-hash scenario, verifying failed queue state and cleanup of the temporary `.modsync.tmp` file.

## [1.0.59] - 2026-03-23

### Changed
- Expanded local-HTTP `DownloadManager` integration coverage with a mixed queue scenario where one file succeeds and another fails, verifying partial progress accounting, restart-state behavior, and batch issue reporting.

## [1.0.58] - 2026-03-23

### Changed
- Added local-HTTP integration coverage for `DownloadManager` queue execution, including successful async download completion, restart-state recording, completion callback behavior, and failed-download issue reporting.

## [1.0.57] - 2026-03-23

### Changed
- Added direct `DownloadManager` post-download coverage for hash verification, invalid-temp-file deletion policy, and temp-file promotion into the final target path.

## [1.0.56] - 2026-03-23

### Changed
- Added direct `DownloadTask` coverage for progress-byte accumulation, retry-attempt counting, completion tracking, and task wiring to manifest entry plus target path.

## [1.0.55] - 2026-03-23

### Changed
- Added cleanup integration coverage for actual obsolete-file deletion, empty-parent cleanup, restart-state recording, and skipping locally modified managed files on temp client fixtures.

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
