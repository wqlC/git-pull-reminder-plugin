# Changelog

All notable changes to the Git Pull Reminder plugin will be documented in this file.

## [1.0.1] - 2026-04-21

### Changed

- Removed `until-build` restriction to support all future IDE versions (251+)

## [1.0.0] - Initial Release

### Added

- Auto-fetch: Automatically executes `git fetch` to get the latest remote status before each commit
- Smart detection: Compares local branch with remote tracking branch to detect unpulled commits
- User-friendly dialog: Shows a clear prompt when remote has new commits
- Flexible options: Pull then commit, commit anyway, or cancel
- Settings panel under `Tools > Git Pull Reminder`
- Internationalization support (English & Chinese)
