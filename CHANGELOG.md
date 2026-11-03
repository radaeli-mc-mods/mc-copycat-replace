# Changelog

All notable changes to this project will be documented in this file.

## [0.3.0] - 2026-11-02

### Added

- Added Connected, Plane, and All types selection modes, switched by scrolling while holding the connected-action key.
- Added automatic use of identical materials from the main inventory for Survival group actions.
- Added Ponder tutorials on Copycat Step and Copycat Panel.
- Added a warning when a Copycat rejects the selected material.
- Added Brazilian Portuguese translations.

### Changed

- Material changes now respect build permissions, with denied positions blocking group propagation.
- Moved the connected-action limit to `copycat_replace-server.toml`, automatically carrying over the previous COMMON value when creating the new configuration.

### Fixed

- Fixed live updates to the connected-action limit on Forge.
- Fixed replacing all matching parts of multipart Copycats when the last material item is consumed.

## [0.2.0] - 2026-10-21

### Added

- Added a Forge 1.20.1 version for Create 6.0.8, with support for Copycats+ 3.0.4.

## [0.1.0] - 2026-09-29

### Added

- Apply, replace, or remove materials across connected Copycat blocks.

## [0.0.2] - 2026-08-22

- Updated the required NeoForge baseline to 21.1.236.

## [0.0.1] - 2026-08-21

- Initial release
