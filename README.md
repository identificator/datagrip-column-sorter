# DataGrip Column Sorter

A lightweight DataGrip plugin that adds convenient column reordering actions directly to the query Result Grid toolbar.

## Features

- Sort result columns alphabetically (A-Z)
- Restore the original column order
- Keep pinned columns grouped first
- Works with multiple queries executed from the same console
- Native toolbar integration inside DataGrip
- Supports IDE actions, so commands can be triggered from the toolbar and via Find Action
- Provides plugin settings for controlling sorting behavior
- Lightweight behavior with no SQL rewriting

## Why

When working with wide query results, it is often useful to quickly reorder columns for scanning and comparison without changing the SQL query itself.

This plugin makes that possible in one click.

## How it works

The plugin changes only the visual order of columns in the Result Grid.

It does **not**:
- modify SQL text
- alter database schema
- change stored table structure
- affect query execution

## Usage

1. Run a query in DataGrip.
2. Open the tabular Result Grid.
3. Use the toolbar actions:
    - **Sort Columns A-Z**
    - **Restore Original Order**
4. Optionally invoke the same commands through **Find Action**.
5. Adjust plugin behavior in **Settings** if needed.

## Screenshots

Before sorting columns

![Before sort](docs/images/before_sort.jpg)

After sorting columns

![After sort](docs/images/after_sort.jpg)

Pinned column (when the option is enabled in Settings, the "id" column is pinned)

![Pinned column](docs/images/pinned_column.jpg)

Before restoring the columns order

![Pinned column](docs/images/before_restore_order.jpg)

After restoring the columns order

![Pinned column](docs/images/after_restore_order.jpg)

## Actions

The plugin provides the following actions in the query Result Grid toolbar:

- **Sort Columns A-Z** — sorts visible columns alphabetically
- **Restore Original Order** — restores the original column order for the current result set

These actions are also available through **Find Action** in DataGrip, so they can be invoked without clicking the toolbar manually.

This makes the plugin convenient both for mouse-driven usage and for keyboard-oriented workflows.

## Settings

The plugin provides configurable settings for column sorting behavior.

To open plugin settings in DataGrip:

1. Open **File | Settings** on Windows/Linux, or **DataGrip | Settings** on macOS
2. Navigate to the plugin settings page **"Column Sorter"**
3. Adjust the available column sorting options

Available settings:

- **Show 'Sort Columns A-Z' button**  
  Shows or hides the toolbar button for alphabetical sorting of columns in the Result Grid.

- **Show 'Restore Original Column Order' button**  
  Shows or hides the toolbar button for restoring the original column order of the current result set.

- **Pinned columns**  
  A case-insensitive list of column names that should be treated as pinned.  
  Add one column name per item.

![Settings screen](docs/images/settings.jpg)

## Current functionality

- Alphabetical sorting of visible result columns
- Restore to the original order for the current result set
- Pinned columns remain grouped first
- Correct handling of multiple result sets executed from the same console
- Integrated action buttons with native JetBrains icons
- Support for IDE actions through the DataGrip action system

## Compatibility

Built for DataGrip on the IntelliJ Platform.

This plugin is currently developed and tested with:

- **DataGrip 2026.1.1** (build **261.22158.354**)
- **JDK 21**
- **Kotlin 2.3.20**
- **IntelliJ Platform Gradle Plugin 2.13.1**

Plugin target:
- **since-build: 261**

## Author

**Alexander Khudoev**  
Website: https://khudoev.dev  
GitHub: https://github.com/identificator

## Source Availability

The source code is published for transparency and reference purposes only.
This repository does not grant permission to copy, modify, redistribute, or commercialize the plugin.

## License

Copyright (c) 2026 Alexander Khudoev. All rights reserved.

This project is source-available, but it is not open source.

You may use the plugin in its original, unmodified form, but you may not copy, modify, redistribute, sublicense, resell, or create derivative works from the plugin or its source code without prior written permission.