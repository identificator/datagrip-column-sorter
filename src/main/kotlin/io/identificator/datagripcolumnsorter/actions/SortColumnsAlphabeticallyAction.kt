package io.identificator.datagripcolumnsorter.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import io.identificator.datagripcolumnsorter.storage.ColumnOrderStorage
import io.identificator.datagripcolumnsorter.table.TableColumnReorderer

class SortColumnsAlphabeticallyAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)

        e.presentation.isVisible = true
        e.presentation.isEnabled = table != null && !TableColumnReorderer.isAlphabeticallySorted(table)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val table = ActionUtils.getActiveTable(e)
        if (table == null) {
            ActionUtils.notifyWarning(e, "Active component is not a result table")
            return
        }

        ColumnOrderStorage.saveOriginalOrderIfNeeded(table)

        val changed = TableColumnReorderer.sortAlphabetically(table)
        if (!changed) {
            ActionUtils.notifyInfo(e, "Not enough columns to sort")
            return
        }

        ActionUtils.notifyInfo(e, "Columns sorted alphabetically")
    }




}