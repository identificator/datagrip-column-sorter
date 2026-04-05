package io.identificator.datagripcolumnsorter.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.identificator.datagripcolumnsorter.storage.ColumnOrderStorage
import io.identificator.datagripcolumnsorter.table.TableColumnReorderer

class SortColumnsAlphabeticallyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        if (table == null) {
            ActionUtils.notifyWarning(e, "Active component is not a result table")
            return
        }

        ColumnOrderStorage.saveOriginalOrderIfAbsent(table)

        val changed = TableColumnReorderer.sortAlphabetically(table)
        if (!changed) {
            ActionUtils.notifyInfo(e, "Not enough columns to sort")
            return
        }

        ActionUtils.notifyInfo(e, "Columns sorted alphabetically")
    }
}