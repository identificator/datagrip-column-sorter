# Changelog

## 1.0.0 - 2026-04-13

### Added
- Added support for sorting columns in additional Result views:
    - Services Result Grid
    - in-editor results
    - table view opened in a separate tab
- Added plugin actions to the column header context menu in regular table view
- Added support for transposed result views

### Changed
- Improved action placement in toolbars and result views
- Updated enable/disable logic for normal and transposed modes separately
- Implemented dedicated transpose sorting logic so row labels and row data are reordered together

### Transposed results view

- Added support for restoring the original field order in transposed result tables
- Added dedicated state storage for the original transposed field order
- Fixed incorrect restore behavior in transposed mode caused by using visible row positions instead of underlying model row indices
- Fixed inconsistent repeated sorting after restore in transposed tables
- Improved transposed sorting reliability for reused result table instances in the same console


## 0.1.0 - 2026-04-06

- Improved result grid toolbar integration
- Added native JetBrains-style action icons
- Added visual separator between plugin actions and built-in toolbar actions
- Improved restore workflow for returning to the original column order
- Added pinned-columns-first behavior during sorting
- Prepared plugin metadata and publication materials for GitHub and JetBrains Marketplace

## 0.0.1 - 2026-04-05

- Created initial DataGrip plugin skeleton
- Added actions for query Result Grid
- Implemented alphabetical column sorting
- Implemented restore original column order
- Added base plugin configuration and MVP functionality