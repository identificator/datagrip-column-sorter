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

class RestoreOriginalColumnOrderAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        val settings = ColumnSorterSettingsState.getInstance().state
        val isTranspose = TableViewModeDetector.isTransposeEnabled(e)

        e.presentation.isVisible = settings.showRestoreButton
        e.presentation.isEnabled = settings.showRestoreButton &&
                table != null &&
                if (isTranspose) {
                    TransposedColumnOrderStorage.hasSavedOrder(table) &&
                            TransposedColumnOrderStorage.matchesCurrentResultSet(table)
                } else {
                    ColumnOrderStorage.hasSavedOrder(table) &&
                            ColumnOrderStorage.matchesCurrentResultSet(table)
                }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        if (table == null) {
            ActionUtils.notifyWarning(e, "Active component is not a result table")
            return
        }

        val isTranspose = TableViewModeDetector.isTransposeEnabled(e)

        if (isTranspose) {
            if (!TransposedColumnOrderStorage.matchesCurrentResultSet(table)) {
                ActionUtils.notifyWarning(e, "Saved column order belongs to another result set")
                return
            }

            val originalOrder = TransposedColumnOrderStorage.getOriginalOrder(table)
            if (originalOrder == null) {
                ActionUtils.notifyInfo(e, "Original column order is not saved")
                return
            }

            val restored = TransposedTableReorderer.restoreOriginalOrder(table, originalOrder)
            if (!restored) {
                ActionUtils.notifyWarning(e, "Failed to restore original column order")
                return
            }

            TransposedColumnOrderStorage.clear(table)
            ActionUtils.notifyInfo(e, "Original column order restored")
            return
        }

        if (!ColumnOrderStorage.matchesCurrentResultSet(table)) {
            ActionUtils.notifyWarning(e, "Saved column order belongs to another result set")
            return
        }

        val originalOrder = ColumnOrderStorage.getOriginalOrder(table)
        if (originalOrder == null) {
            ActionUtils.notifyInfo(e, "Original column order is not saved")
            return
        }

        val restored = TableColumnReorderer.restoreOriginalOrder(table, originalOrder)
        if (!restored) {
            ActionUtils.notifyWarning(e, "Failed to restore original column order")
            return
        }

        ColumnOrderStorage.clear(table)
        ActionUtils.notifyInfo(e, "Original column order restored")
    }
}