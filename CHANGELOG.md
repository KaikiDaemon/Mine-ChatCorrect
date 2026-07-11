# Changelog

All notable changes to Mine-ChatCorrect will be documented in this file.

## [Unreleased]

### Notes

- Current local development version is `0.1.2`.
- Development builds are intentionally distinct from the public `0.1.1` release jar.

### Added

- Added initial unit validation coverage for dictionary word parsing.
- Added initial unit validation coverage for correction suggestion behavior.
- Added malformed/large-line dictionary parser validation.
- Added an in-memory `SpellChecker` constructor for fast validation tests.
- Added `AcceptedWordsStore` to isolate and test accepted-word persistence behavior.
- Added `DictionaryImportLimits` to cap dictionary source and extracted archive sizes.
- Added `ArchiveEntrySafety` validation for archive path traversal protection.
- Added `DictionaryMetadataStore` so imported dictionary enabled/disabled state can persist.
- Added `DictionaryManager` integration tests for local word-list imports, failed-import cleanup, accepted-word persistence, and dictionary enabled-state reloads.
- Added archive import integration tests for `.zip`, `.gz`, `.tar.gz`, and traversal rejection.
- Added dictionary import integration tests for local directories and StarDict `.ifo`/`.idx` data.
- Added unsupported/no-usable-word dictionary import failure and cleanup validation.
- Added download validation helper tests for redirect limits and empty/HTML payload rejection.
- Added large dictionary import tests for oversized local files, zip entries, and gzip output.
- Added tar total-extraction limit accounting and lowered-limit validation hooks for tests.
- Added post-extraction size validation for `.tar.bz2` imports handled by system `tar`.

### Planned

- Add automated validation for dictionary parsing/import.
- Add automated validation for accepted-word storage.
- Add automated validation for correction suggestion behavior.
- Add malformed and large dictionary handling checks.
- Improve dictionary import documentation.
- Review dictionary archive safety.

### Changed

- Changed the default dictionary URL to the en-wl / SCOWL Aspell English `.tar.bz2` release.
- Documented suggested dictionary URLs and third-party dictionary bundling considerations in README.
- Recorded all researched dictionary URL live-test results and confirmed previous failures now pass.
- Added roadmap requirement to pause before uploading/pushing to git for user confirmation.
- Added DICT/FreeDict `.index` and `.index.gz` headword parsing for dictd archives.
- Added `URL_TEST_RESULTS.md` to track live dictionary URL import results.
- Fixed `.tar.gz` archive imports so gzip expansion uses the total archive-size limit instead of the per-entry dictionary limit.
- User confirmed chat focus after correction actions is passed.
- Added Jess research plan file for dictionary import test URLs and default dictionary candidate research.
- Added roadmap requirement to increment version before pushing to git.
- User-tested and confirmed chat action-bar placement, underline alignment, dictionary listing/controls, and local import-folder opener behavior.
- User-tested and confirmed underline alignment, chat action-bar placement, dictionary selection/listing, and local import folder opener behavior.
- Moved chat correction action bar higher to avoid overlapping the vanilla chat input.
- Moved settings status text to a top safe area and increased dictionary/add-word spacing.
- Adjusted settings-screen spacing to avoid status/list/button overlap.
- Re-tuned chat underline positioning after user retest.
- Removed unsafe chat key/typed mixin injections and strengthened render-time chat input refocus.
- Made imported dictionary rows clickable and improved detected local-file install labeling.
- Adjusted chat underline positioning for better text alignment.
- Added local dictionary drop-folder install workflow from the settings screen.
- Added explicit previous/next dictionary selection controls.
- Added delayed chat-input refocus after correction actions.
- Adjusted spell-check underline alignment.
- Improved chat action-bar focus recovery after correction actions.
- Improved dictionary enabled/disabled labeling and multi-dictionary visibility in settings.
- Verified `./gradlew test` and `./gradlew build` with Java 21.
- Added logging for dictionary/config load, save, fallback, tar-probing, and delete failures that were previously silent.
- Expanded README dictionary import, accepted-word, compatibility, install, and build-output documentation.
- Added cleanup for failed dictionary imports so unusable imports do not leave stale import directories.
- Added a maximum redirect count for dictionary downloads.
- Replaced an unbounded direct dictionary file read with the size-limited dictionary reader.

## [0.1.1] - 2026-07-09

### Added

- Client-side Minecraft chat spell-check assistance.
- Highlighting for likely misspelled chat input words.
- Replacement suggestion controls in the chat UI.
- Manual replacement support.
- Accepted/custom word support.
- Optional auto-correction behavior.
- In-game settings GUI.
- External dictionary import and management support.
- Imported dictionary enable/disable/remove support.
- GUI for browsing, editing, and removing additional accepted words.

### Notes

- Built for Minecraft `1.21.1`.
- Tested with NeoForge `21.1.172`.
- Client-side only; servers do not need to install it.

[Unreleased]: https://github.com/KaikiDaemon/Mine-ChatCorrect/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/KaikiDaemon/Mine-ChatCorrect/releases/tag/v0.1.1
