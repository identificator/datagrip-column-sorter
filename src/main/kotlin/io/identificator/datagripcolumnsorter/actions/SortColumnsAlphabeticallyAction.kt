package io.identificator.datagripcolumnsorter.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.identificator.datagripcolumnsorter.settings.ColumnSorterSettingsState
import io.identificator.datagripcolumnsorter.storage.ColumnOrderStorage
import io.identificator.datagripcolumnsorter.storage.TransposedColumnOrderStorage
import io.identificator.datagripcolumnsorter.table.TableColumnReorderer
import io.identificator.datagripcolumnsorter.table.TableViewModeDetector
import io.identificator.datagripcolumnsorter.table.TransposedTableReorderer

class SortColumnsAlphabeticallyAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        val settings = ColumnSorterSettingsState.getInstance().state
        val isTranspose = TableViewModeDetector.isTransposeEnabled(e)

        e.presentation.isVisible = settings.showSortButton

        val isEnabled = settings.showSortButton
                && table != null
                && if (isTranspose) {
            !TransposedTableReorderer.isAlphabeticallySorted(table)
        } else {
            !TableColumnReorderer.isAlphabeticallySorted(table)
        }

        e.presentation.isEnabled = isEnabled
    }


    override fun actionPerformed(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        if (table == null) {
            ActionUtils.notifyWarning(e, "Active component is not a result table")
            return
        }

        val isTranspose = TableViewModeDetector.isTransposeEnabled(e)

        val changed = if (isTranspose) {
            TransposedColumnOrderStorage.saveOriginalOrderIfNeeded(table)
            TransposedTableReorderer.sortAlphabetically(table)
        } else {
            ColumnOrderStorage.saveOriginalOrderIfNeeded(table)
            TableColumnReorderer.sortAlphabetically(table)
        }

        if (!changed) {
            ActionUtils.notifyInfo(e, "Not enough columns to sort")
            return
        }

        ActionUtils.notifyInfo(e, "Columns sorted alphabetically")
    }
}