package io.identificator.datagripcolumnsorter.table

import java.awt.Component
import java.awt.Container
import java.lang.reflect.Method
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JViewport

object TransposedRowHeaderReflectionUtils {
    private const val TABLE_RESULT_ROW_HEADER_CLASS =
        "com.intellij.database.run.ui.table.TableResultRowHeader"

    fun readVisibleFieldHeaders(table: JTable?): List<String> {
        if (table == null) {
            return emptyList()
        }

        val rowHeader = findRowHeader(table) ?: return emptyList()
        val rowCount = safeCall { table.rowCount } ?: return emptyList()

        val getCellRendererMethod = findMethod(rowHeader.javaClass, "getCellRenderer")
            ?: return emptyList()

        val renderer = safeCall { getCellRendererMethod.invoke(rowHeader) } ?: return emptyList()
        val getRendererComponentMethod = findMethod(
            renderer.javaClass,
            "getRendererComponent",
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        ) ?: return emptyList()

        val headers = mutableListOf<String>()

        var row = 0
        while (row < rowCount) {
            val component = safeCall {
                getRendererComponentMethod.invoke(renderer, row, true) as? Component
            }

            val text = extractBestText(component).orEmpty()
            headers += text
            row += 1
        }

        return headers
    }

    private fun findRowHeader(table: JTable): JComponent? {
        val viewport = table.parent as? JViewport ?: return null
        val scrollPane = viewport.parent as? JScrollPane ?: return null

        val rowHeaderView = scrollPane.rowHeader?.view
        val direct = findFirstByClassName(rowHeaderView, TABLE_RESULT_ROW_HEADER_CLASS)
        if (direct != null) {
            return direct as? JComponent
        }

        val parent = scrollPane.parent
        val nearby = findFirstByClassName(parent, TABLE_RESULT_ROW_HEADER_CLASS)
        return nearby as? JComponent
    }

    private fun extractBestText(component: Component?): String? {
        if (component == null) {
            return null
        }

        val labels = mutableListOf<JLabel>()
        collectLabels(component, labels)

        if (labels.isEmpty()) {
            return null
        }

        val texts = labels
            .mapNotNull { it.text?.trim() }
            .filter { it.isNotEmpty() && it != "    " }

        if (texts.isEmpty()) {
            return null
        }

        return texts.maxByOrNull { it.length }
    }

    private fun collectLabels(component: Component, out: MutableList<JLabel>) {
        if (component is JLabel) {
            out += component
        }

        if (component is Container) {
            val children = component.components
            var index = 0
            while (index < children.size) {
                collectLabels(children[index], out)
                index += 1
            }
        }
    }

    private fun findFirstByClassName(component: Component?, targetClassName: String): Component? {
        if (component == null) {
            return null
        }

        if (component.javaClass.name == targetClassName) {
            return component
        }

        if (component is Container) {
            val children = component.components
            var index = 0
            while (index < children.size) {
                val found = findFirstByClassName(children[index], targetClassName)
                if (found != null) {
                    return found
                }
                index += 1
            }
        }

        return null
    }

    private fun findMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>?): Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            val methods = current.declaredMethods
            var index = 0
            while (index < methods.size) {
                val method = methods[index]
                if (method.name == name && parametersMatch(method.parameterTypes, parameterTypes)) {
                    method.isAccessible = true
                    return method
                }
                index += 1
            }
            current = current.superclass
        }
        return null
    }

    private fun parametersMatch(actual: Array<Class<*>>, expected: Array<out Class<*>?>): Boolean {
        if (actual.size != expected.size) {
            return false
        }

        var index = 0
        while (index < actual.size) {
            val expectedType = expected[index] ?: return false
            if (actual[index] != expectedType) {
                return false
            }
            index += 1
        }

        return true
    }

    private fun <T> safeCall(block: () -> T): T? {
        return try {
            block()
        } catch (_: Throwable) {
            null
        }
    }
}