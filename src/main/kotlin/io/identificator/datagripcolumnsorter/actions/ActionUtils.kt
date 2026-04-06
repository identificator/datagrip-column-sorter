package io.identificator.datagripcolumnsorter.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import java.awt.Component
import java.awt.Container
import java.awt.KeyboardFocusManager
import javax.swing.JTable

object ActionUtils {
    fun getActiveTable(e: AnActionEvent): JTable? {
        val contextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(e.dataContext)
        val fromContext = findTableNearby(contextComponent)
        if (fromContext != null) {
            return fromContext
        }

        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        return findTableNearby(focusOwner)
    }

    private fun findTableNearby(component: Component?): JTable? {
        if (component == null) {
            return null
        }

        val inSelfOrChildren = findFirstTable(component)
        if (inSelfOrChildren != null) {
            return inSelfOrChildren
        }

        var parent = component.parent
        while (parent != null) {
            val found = findFirstTable(parent)
            if (found != null) {
                return found
            }
            parent = parent.parent
        }

        return null
    }

    private fun findFirstTable(component: Component?): JTable? {
        if (component == null) {
            return null
        }

        if (component is JTable) {
            return component
        }

        if (component is Container) {
            val children = component.components
            var index = 0
            while (index < children.size) {
                val found = findFirstTable(children[index])
                if (found != null) {
                    return found
                }
                index += 1
            }
        }

        return null
    }

    fun notifyInfo(e: AnActionEvent, message: String) {
        val project = e.project ?: return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Column Sorter")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun notifyWarning(e: AnActionEvent, message: String) {
        val project = e.project ?: return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Column Sorter")
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
}