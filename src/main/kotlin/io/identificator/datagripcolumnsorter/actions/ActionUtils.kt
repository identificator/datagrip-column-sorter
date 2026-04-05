package io.identificator.datagripcolumnsorter.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import javax.swing.JTable

object ActionUtils {
    fun getActiveTable(e: AnActionEvent): JTable? {
        val contextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(e.dataContext)
        return contextComponent as? JTable
    }

    fun notifyInfo(e: AnActionEvent, message: String) {
        val project = e.project ?: return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("DataGrip Column Sorter")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun notifyWarning(e: AnActionEvent, message: String) {
        val project = e.project ?: return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("DataGrip Column Sorter")
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
}