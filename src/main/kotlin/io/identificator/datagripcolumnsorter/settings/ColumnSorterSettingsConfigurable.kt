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
                                    "The remaining columns are sorted alphabetically."
                        )
                }
            }

            onApply {
                settings.pinnedColumnNames = pinnedListModel.items
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .toMutableList()
            }

            onReset {
                pinnedListModel.removeAll()
                settings.pinnedColumnNames.forEach { pinnedListModel.add(it) }
            }

            onIsModified {
                val currentPinned = pinnedListModel.items
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                settings.pinnedColumnNames != currentPinned
            }
        }
    }
}