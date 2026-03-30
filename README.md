# SyncBridge

SyncBridge is a universal Forge 1.20.1 mod that lets a server distribute mods and resource files to connecting clients while Minecraft is running.

Current mod version: `1.0.94`

## Requirements

- Java 17
- Gradle 8.x or the Gradle wrapper
- Minecraft Forge 1.20.1 / Forge 47.4.18+

## Build

```bash
./gradlew build
```

The built universal jar is created under `build/libs/`.

## CI

The repository includes a GitHub Actions workflow that runs the shared `./scripts/run-release-smoke.sh` checks on pushes and pull requests.
Minimal automated tests are available through `./gradlew test`.
Successful CI runs also upload the built jar from `build/libs/` as a workflow artifact.
CI and release flows now also generate and publish a matching `.sha256` checksum file for the jar.
CI also uploads the HTML test report from `build/reports/tests/test/`.
Workflow concurrency now cancels superseded in-progress CI runs on the same ref, and both CI/release jobs have explicit timeouts.
For local release prep, `./scripts/run-release-smoke.sh vX.Y.Z` now bundles the same core checks plus version consistency, expected-jar verification, version metadata validation inside the jar, and checksum generation, and the same helper is used by GitHub Actions.
Test coverage now includes cleanup decisions, managed-state naming, server sync status caching, category mapping, file path filtering rules, protected-file detection, manifest entry copy semantics, sync comparison edge cases, packet chunking/reassembly, manifest URL resolution, MOTD metadata decoding, restart-state bookkeeping, download-flow error messaging, and sync progress state resolution in addition to path safety and manifest JSON.
The performance-oriented file hash cache is now also covered for persistence, invalidation, scope cleanup, and client/server scope separation.
Managed-state persistence is now also covered beyond naming alone, including round-trip save/load behavior and invalid-json fallback.
Hashing behavior is now also covered directly with known SHA-256 vectors and format checks, which strengthens the base layer under manifest generation and file verification.
Client/runtime state holders are now also covered, including pre-join readiness bookkeeping, sync issue message normalization, and bounded log buffering for the sync UI.
Client file scanning is now also covered directly on temp directory fixtures, including skipped extensions, generated relative paths, category-specific restart flags, and missing-root behavior.
Manifest generation is now also covered directly on temp repository fixtures, including written manifest-copy output and multi-category entry generation.
A mini integration test now also covers the core sync pipeline path from generated server manifest through client scan into comparison results.
Cleanup is now also covered beyond decision logic, including real file deletion, empty-directory pruning, restart-state recording, and skipping locally modified managed files.
Download progress bookkeeping is now also covered directly through `DownloadTask`, including downloaded-byte accumulation, retry counting, and completion state.
The post-download finalize path is now also covered directly, including hash verification, invalid-temp-file cleanup policy, and temp-to-final promotion.
`DownloadManager` queue execution is now also covered against a local HTTP server, including successful async completion and failed-download issue propagation.
That download integration layer now also covers mixed queues with partial success, so batch issue handling and completed-task accounting are checked against a more realistic case.
It now also covers the “HTTP succeeded but SHA-256 is invalid” path, including cleanup of the temporary `.modsync.tmp` file.
The manifest HTTP stage is now also covered against local HTTP servers, including candidate fallback and attempted-URL failure reporting.
The public-manifest transformation step is now also covered directly, including generated download URLs and the empty fallback when no cached manifest exists.
The manifest-fetch path is now also covered end-to-end from `serverAddress` plus discovered/configured ports into actual candidate selection and HTTP retrieval.
`ServerSyncStatusCache` now also has direct coverage for local scan caching and invalidation, which helps protect the pre-join flow from unnecessary rescans and stale cache reuse.
`PreJoinSyncManager` now also has direct orchestration coverage for deciding whether sync can continue immediately, requires downloads first, or should avoid auto-continuation when connect-after-sync is disabled.
`ClientBootstrap` now also has direct orchestration coverage for the post-login branch, including local-session skipping, duplicate-handshake suppression after pre-join sync, and the regular multiplayer handshake path.
HTTP file delivery now percent-encodes relative paths, so downloads with spaces, `+`, `%`, and similar URL-sensitive characters in file names work correctly through both manifest-delivery paths.
The embedded HTTP file server now also has direct coverage for approved-entry resolution on encoded paths, which helps protect that new filename fix from regressions.
The packet-based handshake path now also has direct coverage for encoded download URLs and `start-download` selection rules, so both delivery paths are checked against the same filename and comparison behavior.
That handshake coverage now also includes the `start-download` decision plan itself, so “already synchronized”, “download required”, and “manifest missing fallback” behavior are all locked in explicitly.
The server-side packet response path is now also covered as a single unit, so manifest URL encoding and required-download calculation stay aligned when the client sends its file list.
That same packet path now also has explicit fallback coverage for the “manifest cache is not ready yet” case, so the server normalizes to an empty manifest before answering instead of relying on an inline branch.
A mini packet-handshake integration test now also ties the server response and client download-decision planners together, so encoded download URLs and no-download cases are checked across the full non-Forge round trip.
That packet coverage now also goes through chunk splitting and reassembly for long encoded file names, which protects the payload path used by real packet transport.
The mini packet-handshake integration layer now also covers the empty-manifest-cache fallback, so the client-side no-download path is checked even when the server cache is not ready yet.
Chunked packet coverage now also checks that a stale `serverSuggested` payload does not override a matching chunked manifest on the client side.
Chunk reassembly itself is now stricter too: invalid order and mismatched chunk-count sequences are discarded instead of being merged into corrupted payloads.
Manifest JSON parsing is now also normalized defensively, so `null` manifests or `entries: null` payloads collapse to empty manifest data instead of leaking `null` deeper into the sync flow.
That normalization now also filters out `null` entries inside manifest payloads and entry arrays before later sync stages consume them.
Malformed entries without a category or usable relative path are now filtered out too, which protects several runtime paths that assume `getIdentityKey()` is safe to call.
Entries without a usable SHA-256 are now filtered out as well, which avoids malformed payloads reaching comparison and post-download verification logic.
The fallback `serverSuggested` path is now hardened too: entries without usable download metadata are discarded before the client decides to start downloads.
`ManifestData` now also protects its own `entries` list against `null` assignment and accidental outside mutation, which makes the manifest container itself more stable across runtime paths.
Version tags like `v1.0.17` can now trigger a GitHub Release workflow that runs the shared smoke helper and publishes the built jar.
Release notes are now rendered from the top matching changelog section before the GitHub Release is created.
The release workflow now also verifies that the git tag, `mod_version`, and the top changelog entry all describe the same version before publishing.
The Gradle build script has also been cleaned up toward more current task/publishing conventions.
The Gradle test-framework deprecation has been addressed by using explicit JUnit Jupiter API, engine, and platform launcher dependencies, and the local `build --warning-mode all` path is currently clean.
The Forge 47.4.18 compile path is now also clean of the earlier removal warnings after updating `ResourceLocation` creation and config registration to the newer API-safe patterns.
Manifest HTTP fallback resolution is now covered by unit tests for direct URLs, configured public base URLs, and IPv6 hosts.
Integrated/local server sessions no longer trigger the dedicated-server SyncBridge handshake timeout path, which avoids false "missing SyncBridge/Forge" disconnects during local play or local diagnostics.
Default network tolerance is also higher now: pre-join manifest fetches and HTTP file downloads wait longer before timing out, which helps large remote modpacks sync more reliably on slower hosts.
If a manifest still contains a dead `public_http_base_url`, the client now retries downloads against the selected server host plus the discovered hidden SyncBridge HTTP port before giving up.
After successful pre-join sync on a dedicated server, the client now still sends a lightweight SyncBridge acknowledgement on login so the server does not kick an already synchronized player for a missing handshake timeout.
SyncBridge does not self-update its own jar at runtime. Replace the distributed `modsync` jar manually when publishing a new build to clients or servers.
Dedicated-server handshake tracking now also tolerates out-of-order arrival between the first client `hello` and pending registration, which avoids intermittent late kicks for already connected players.
The default dedicated-server handshake timeout is now 120 seconds, and timeout handling is operator-driven by default: instead of immediately kicking, SyncBridge logs the timeout to console, notifies online admins, and waits for an explicit `allow` or `kick` decision unless auto-kick is enabled in config.

## Server Repository Layout

Server mods are synchronized directly from the active server `mods/` folder.

Client-only and non-mod content stays in `sync_repo/`:

```text
server/
  mods/
  config/
  sync_repo/
    resourcepacks/
    shaderpacks/
    configs/
    optional_client/
```

The mod scans `mods/` plus `sync_repo/` and writes a generated manifest to `config/modsync-manifest.json`.

## Runtime Flow

1. Client joins the server and sends a handshake.
2. Client scans installed files and sends metadata to the server.
3. Server generates the manifest and returns it to the client.
4. Client compares local files with the manifest.
5. Missing or outdated files download in background threads over the embedded HTTP server.
6. The Connect action only validates sync state and continues into the server if the client is already up to date.
7. The Download Mods action is the explicit flow that downloads missing or outdated files without auto-connecting.
8. Files previously installed by SyncBridge are removed if they no longer exist in that server's manifest.
9. If restart-required files were changed, the client sees a restart prompt.

## Configuration

An example configuration file is included at `config/modsync.toml`.

Important options:

- `server_http_port`
- `server_http_bind`
- `public_http_base_url`
- `optional_client_target`
- `download_threads`
- `retry_count`
- `handshake_timeout_seconds`
- `auto_kick_on_handshake_timeout`
- `verify_hash_after_download`
- `skip_file_extensions`

## Reverse Proxy

If you do not want to expose `server_http_port` directly, put the embedded HTTP server behind a reverse proxy and set:

```toml
public_http_base_url = "https://your-domain.example/modsync"
```

The server will publish download URLs using that base URL.

The proxy must forward these paths to the internal SyncBridge HTTP server:

- `/manifest`
- `/files/...`

If `public_http_base_url` is empty, SyncBridge falls back to direct links using `server_http_port`.
When `public_http_base_url` is set, SyncBridge now uses it consistently for generated download links in both manifest delivery paths.
Detailed deployment examples are documented in `docs/Server_Setup_RU.md`.

## Notes

- Files are downloaded through temporary files before replacement when enabled.
- Hashes are verified with SHA-256.
- Path traversal is blocked on both manifest generation and HTTP serving.
- Download URLs now safely encode file-path segments before transport and decode them again on the embedded HTTP server.
- Cleanup only removes files that still match the last SyncBridge-managed version, which reduces the risk of deleting locally replaced files.
- The mod logs operational events to `logs/modsync.log` when enabled.
- Network and download failures are surfaced both in the log panel and directly on the sync progress screen.
- Client and server scans avoid some duplicate filesystem metadata reads to behave a bit better on large file sets.
- The sync progress screen now shows a clearer high-level state so players can tell whether SyncBridge is checking, downloading, finished, failed, or waiting for restart.

## Documentation

- `docs/README.md`
- `docs/Server_Setup_RU.md`
- `docs/Player_Guide_RU.md`
- `docs/Discord_Templates_RU.md`
- `docs/ModSync_FAQ_Discord.md`
- `docs/Roadmap_RU.md`
- `docs/Smoke_Checklist_RU.md`
- `docs/Operations_Guide_RU.md`
- `docs/Release_Guide_RU.md`
- `CHANGELOG.md`

## Release Process

For every change in this repository:

- bump `mod_version` in `gradle.properties`
- add an entry to `CHANGELOG.md` with the new version and a short summary of what changed
- update documentation in `README.md` and/or `docs/` when behavior, setup, UI, or workflows changed
- optionally create and push a tag like `v1.0.17` to publish a GitHub Release artifact

## CurseForge Publishing

CurseForge project creation still has to be done once in the author web interface.

After that, tagged releases can be uploaded automatically if the repository has:

- `CURSEFORGE_TOKEN` in GitHub Actions secrets
- `CURSEFORGE_PROJECT_ID` in GitHub Actions variables
- optional `CURSEFORGE_GAME_VERSIONS`, defaulting to `1.20.1,Forge`

Prepared project-page text is included in:

Note: the public name is now `SyncBridge`, but the technical `modsync` mod id is intentionally kept for compatibility with existing installations.
