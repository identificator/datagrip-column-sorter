package io.identificator.datagripcolumnsorter.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class ColumnSorterSettingsConfigurable : BoundConfigurable("Column Sorter") {
    override fun createPanel(): DialogPanel {
        val settings = ColumnSorterSettingsState.getInstance().state

        val pinnedListModel = CollectionListModel<String>()
        settings.pinnedColumnNames.forEach { pinnedListModel.add(it) }

        val pinnedList = JBList(pinnedListModel)

        val pinnedListPanel = ToolbarDecorator.createDecorator(pinnedList)
            .setAddAction {
                val value = Messages.showInputDialog(
                    pinnedList,
                    "Enter a column name to pin.",
                    "Add Pinned Column",
                    null
                )?.trim()

                if (!value.isNullOrEmpty() && !pinnedListModel.items.contains(value)) {
                    pinnedListModel.add(value)
                }
            }
            .setRemoveAction {
                val selected = pinnedList.selectedValuesList
                selected.forEach { pinnedListModel.remove(it) }
            }
            .setEditAction {
                val selectedIndex = pinnedList.selectedIndex
                if (selectedIndex < 0) {
                    return@setEditAction
                }

                val oldValue = pinnedListModel.getElementAt(selectedIndex)
                val newValue = Messages.showInputDialog(
                    pinnedList,
                    "Edit pinned column name.",
                    "Edit Pinned Column",
                    null,
                    oldValue,
                    null
                )?.trim()

                if (!newValue.isNullOrEmpty()) {
                    pinnedListModel.setElementAt(newValue, selectedIndex)
                }
            }
            .disableUpDownActions()
            .createPanel()

        val typeRuleListModel = CollectionListModel<String>()
        settings.typeSortRules.forEach { rule ->
            typeRuleListModel.add(formatTypeRule(rule))
        }

        val typeRuleList = JBList(typeRuleListModel)

        val typeRuleListPanel = ToolbarDecorator.createDecorator(typeRuleList)
            .setAddAction {
                val newRule = promptForTypeRule(typeRuleList, null)
                if (newRule != null) {
                    val formatted = formatTypeRule(newRule)
                    if (!typeRuleListModel.items.contains(formatted)) {
                        typeRuleListModel.add(formatted)
                    }
                }
            }
            .setRemoveAction {
                val selected = typeRuleList.selectedValuesList
                selected.forEach { typeRuleListModel.remove(it) }
            }
            .setEditAction {
                val selectedIndex = typeRuleList.selectedIndex
                if (selectedIndex < 0) {
                    return@setEditAction
                }

                val current = parseTypeRule(typeRuleListModel.getElementAt(selectedIndex)) ?: return@setEditAction
                val updated = promptForTypeRule(typeRuleList, current) ?: return@setEditAction
                typeRuleListModel.setElementAt(formatTypeRule(updated), selectedIndex)
            }
            .createPanel()

        return panel {
            row {
                checkBox("Show 'Sort Columns A-Z' button")
                    .bindSelected(settings::showSortButton)
            }

            row {
                checkBox("Show 'Restore Original Column Order' button")
                    .bindSelected(settings::showRestoreButton)
            }

            group("Pinned columns") {
                row {
                    cell(pinnedListPanel)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment("Add one column name per item. Matching is case-insensitive.")
                }.resizableRow()

                row {
                    checkBox("Pin matching columns first")
                        .bindSelected(settings::enablePinnedColumnsFirst)
                        .comment(
                            "If enabled, columns whose names match the list above are moved to the beginning. " +
                                    "The remaining columns are sorted according to the active sort mode."
                        )
                }
            }

            group("Sort by data type") {
                row {
                    checkBox("Sort columns by data type first")
                        .bindSelected(settings::enableSortByTypeFirst)
                        .comment(
                            "Applies to the regular result grid. " +
                                    "Columns are grouped by matching JDBC type and type name, then sorted alphabetically inside each group."
                        )
                }

                row {
                    cell(typeRuleListPanel)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment(
                            "Ordered rules for PostgreSQL. Format: JDBC type | type name. " +
                                    "Exact match is preferred. If exact match is missing, fallback is by JDBC type."
                        )
                }.resizableRow()
            }

            onApply {
                settings.pinnedColumnNames = pinnedListModel.items
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .toMutableList()

                settings.typeSortRules = typeRuleListModel.items
                    .mapNotNull { parseTypeRule(it) }
                    .distinct()
                    .toMutableList()
            }

            onReset {
                pinnedListModel.removeAll()
                settings.pinnedColumnNames.forEach { pinnedListModel.add(it) }

                typeRuleListModel.removeAll()
                settings.typeSortRules.forEach { rule ->
                    typeRuleListModel.add(formatTypeRule(rule))
                }
            }

            onIsModified {
                val currentPinned = pinnedListModel.items
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                val currentTypeRules = typeRuleListModel.items
                    .mapNotNull { parseTypeRule(it) }
                    .distinct()

                settings.pinnedColumnNames != currentPinned ||
                        settings.typeSortRules != currentTypeRules
            }
        }
    }

    private fun promptForTypeRule(
        parent: JBList<String>,
        initialRule: ColumnSorterSettingsState.TypeSortRule?
    ): ColumnSorterSettingsState.TypeSortRule? {
        val jdbcTypeValue = Messages.showInputDialog(
            parent,
            "Enter JDBC type number.",
            if (initialRule == null) "Add Type Sort Rule" else "Edit Type Sort Rule",
            null,
            initialRule?.jdbcType?.toString().orEmpty(),
            null
        )?.trim() ?: return null

        val jdbcType = jdbcTypeValue.toIntOrNull()
        if (jdbcType == null) {
            Messages.showErrorDialog(parent, "JDBC type must be a valid integer.", "Invalid JDBC Type")
            return null
        }

        val typeName = Messages.showInputDialog(
            parent,
            "Enter database type name.",
            if (initialRule == null) "Add Type Sort Rule" else "Edit Type Sort Rule",
            null,
            initialRule?.typeName.orEmpty(),
            null
        )?.trim() ?: return null

        if (typeName.isEmpty()) {
            Messages.showErrorDialog(parent, "Type name must not be empty.", "Invalid Type Name")
            return null
        }

        return ColumnSorterSettingsState.TypeSortRule(
            jdbcType = jdbcType,
            typeName = typeName
        )
    }

    private fun formatTypeRule(rule: ColumnSorterSettingsState.TypeSortRule): String {
        return "${rule.jdbcType} | ${rule.typeName}"
    }

    private fun parseTypeRule(value: String): ColumnSorterSettingsState.TypeSortRule? {
        val parts = value.split("|", limit = 2)
        if (parts.size != 2) {
            return null
        }

        val jdbcType = parts[0].trim().toIntOrNull() ?: return null
        val typeName = parts[1].trim()
        if (typeName.isEmpty()) {
            return null
        }

        return ColumnSorterSettingsState.TypeSortRule(
            jdbcType = jdbcType,
            typeName = typeName
        )
    }
}