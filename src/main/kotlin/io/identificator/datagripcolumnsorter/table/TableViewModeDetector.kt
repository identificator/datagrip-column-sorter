package io.identificator.datagripcolumnsorter.table

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

object TableViewModeDetector {
    private const val TRANSPOSE_ACTION_ID = "Console.TableResult.Transpose"

    fun isTransposeEnabled(e: AnActionEvent): Boolean {
        val action = ActionManager.getInstance().getAction(TRANSPOSE_ACTION_ID)
        val toggleAction = action as? ToggleAction ?: return false
        return toggleAction.isSelected(e)
    }
}