# Changelog

All notable changes to this project are documented in this file.

The format is intentionally simple:
- Every repository change must bump the mod version.
- Every repository change must add a matching changelog entry.
- Documentation must be updated in the same change whenever behavior, setup, or usage changes.

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
