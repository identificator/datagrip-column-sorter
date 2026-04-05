package io.identificator.datagripcolumnsorter.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.identificator.datagripcolumnsorter.storage.ColumnOrderStorage
import io.identificator.datagripcolumnsorter.table.TableColumnReorderer

class RestoreOriginalColumnOrderAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        if (table == null) {
            ActionUtils.notifyWarning(e, "Active component is not a result table")
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

        ActionUtils.notifyInfo(e, "Original column order restored")
    }
}