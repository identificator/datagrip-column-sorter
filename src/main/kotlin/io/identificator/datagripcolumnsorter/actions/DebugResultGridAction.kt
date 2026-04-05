package io.identificator.datagripcolumnsorter.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import java.util.Locale
import javax.swing.JTable

class DebugResultGridAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val contextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(e.dataContext)

        if (contextComponent !is JTable) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DataGrip Column Sorter")
                .createNotification(
                    "Active component is not JTable: ${contextComponent?.javaClass?.name ?: "null"}",
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }

        val table = contextComponent
        val columnModel = table.columnModel
        val count = columnModel.columnCount

        if (count < 2) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DataGrip Column Sorter")
                .createNotification(
                    "Not enough columns to sort",
                    NotificationType.INFORMATION
                )
                .notify(project)
            return
        }

        val originalHeaders = mutableListOf<String>()
        var originalIndex = 0
        while (originalIndex < count) {
            originalHeaders += columnModel.getColumn(originalIndex).headerValue?.toString().orEmpty()
            originalIndex += 1
        }

        val sortedHeaders = originalHeaders
            .mapIndexed { index, header -> index to header }
            .sortedWith(compareBy({ it.second.lowercase(Locale.getDefault()) }, { it.first }))
            .map { it.second }

        var targetIndex = 0
        while (targetIndex < sortedHeaders.size) {
            val expectedHeader = sortedHeaders[targetIndex]
            val currentIndex = findColumnIndexByHeader(table, expectedHeader, targetIndex)
            if (currentIndex >= 0 && currentIndex != targetIndex) {
                columnModel.moveColumn(currentIndex, targetIndex)
            }
            targetIndex += 1
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("DataGrip Column Sorter")
            .createNotification(
                "Columns sorted alphabetically",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    private fun findColumnIndexByHeader(
        table: JTable,
        header: String,
        fallbackIndex: Int
    ): Int {
        val columnModel = table.columnModel
        var index = 0
        while (index < columnModel.columnCount) {
            val currentHeader = columnModel.getColumn(index).headerValue?.toString().orEmpty()
            if (currentHeader == header) {
                return index
            }
            index += 1
        }

        return if (fallbackIndex < columnModel.columnCount) fallbackIndex else -1
    }
}