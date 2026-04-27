package io.identificator.datagripcolumnsorter.table

import io.identificator.datagripcolumnsorter.settings.ColumnSorterSettingsState
import java.awt.Rectangle
import java.lang.reflect.Method
import java.util.Locale
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.SwingUtilities

object TableColumnReorderer {
    private data class ColumnDescriptor(
        val viewIndex: Int,
        val header: String,
        val jdbcType: Int?,
        val typeName: String?
    )

    fun sortAlphabetically(table: JTable): Boolean {
        val columnModel = table.columnModel
        val count = columnModel.columnCount

        if (count < 2) {
            return false
        }

        val settings = ColumnSorterSettingsState.getInstance().state
        val descriptors = readColumnDescriptors(table)
        val targetHeaders = buildTargetOrder(descriptors, settings)
        val changed = applyHeaderOrder(table, targetHeaders)

        if (changed) {
            resetSelectionAndScroll(table)
        }

        return changed
    }

    fun restoreOriginalOrder(table: JTable, originalHeaders: List<String>): Boolean {
        if (originalHeaders.isEmpty()) {
            return false
        }

        val changed = applyHeaderOrder(table, originalHeaders)
        if (changed) {
            resetSelectionAndScroll(table)
        }

        return changed
    }

    fun isAlphabeticallySorted(table: JTable): Boolean {
        val settings = ColumnSorterSettingsState.getInstance().state
        val descriptors = readColumnDescriptors(table)
        val targetHeaders = buildTargetOrder(descriptors, settings)
        val currentHeaders = descriptors.map { it.header }

        return currentHeaders == targetHeaders
    }

    private fun buildTargetOrder(
        descriptors: List<ColumnDescriptor>,
        settings: ColumnSorterSettingsState.State
    ): List<String> {
        val sorted = descriptors.sortedWith(
            compareBy<ColumnDescriptor>(
                { pinnedRank(it, settings) },
                { typeRank(it, settings) },
                { it.header.lowercase(Locale.getDefault()) },
                { it.viewIndex }
            )
        )

        return sorted.map { it.header }
    }

    private fun pinnedRank(
        descriptor: ColumnDescriptor,
        settings: ColumnSorterSettingsState.State
    ): Int {
        if (!settings.enablePinnedColumnsFirst) {
            return 1
        }

        val pinnedNames = settings.pinnedColumnNames
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
            .toSet()

        val headerLower = descriptor.header.trim().lowercase(Locale.getDefault())
        return if (headerLower in pinnedNames) 0 else 1
    }

    private fun typeRank(
        descriptor: ColumnDescriptor,
        settings: ColumnSorterSettingsState.State
    ): Int {
        if (!settings.enableSortByTypeFirst) {
            return 0
        }

        val jdbcType = descriptor.jdbcType ?: return Int.MAX_VALUE
        val typeName = descriptor.typeName?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        val rules = settings.typeSortRules

        var index = 0
        while (index < rules.size) {
            val rule = rules[index]
            if (rule.jdbcType == jdbcType &&
                rule.typeName.trim().lowercase(Locale.getDefault()) == typeName
            ) {
                return index
            }
            index += 1
        }

        index = 0
        while (index < rules.size) {
            val rule = rules[index]
            if (rule.jdbcType == jdbcType) {
                return index
            }
            index += 1
        }

        return Int.MAX_VALUE
    }

    private fun readColumnDescriptors(table: JTable): List<ColumnDescriptor> {
        val columnModel = table.columnModel
        val count = columnModel.columnCount
        val descriptors = mutableListOf<ColumnDescriptor>()

        val model = table.model
        val createColumnMethod = findCreateColumnMethod(model)

        var viewIndex = 0
        while (viewIndex < count) {
            val header = columnModel.getColumn(viewIndex).headerValue?.toString().orEmpty()

            var jdbcType: Int? = null
            var typeName: String? = null

            if (createColumnMethod != null) {
                val modelIndex = table.convertColumnIndexToModel(viewIndex)
                val tableResultViewColumn = safeCall {
                    createColumnMethod.invoke(model, modelIndex)
                }
                val rawColumn = readFieldValue(tableResultViewColumn, "myColumn")

                jdbcType = readProperty(rawColumn, "getType") as? Int
                typeName = readProperty(rawColumn, "getTypeName")?.toString()
            }

            descriptors += ColumnDescriptor(
                viewIndex = viewIndex,
                header = header,
                jdbcType = jdbcType,
                typeName = typeName
            )

            viewIndex += 1
        }

        return descriptors
    }

    private fun findCreateColumnMethod(model: Any): Method? {
        return model.javaClass.methods.firstOrNull { current ->
            current.name == "createColumn" &&
                    current.parameterCount == 1 &&
                    current.parameterTypes[0] == Int::class.javaPrimitiveType
        }
    }

    private fun readFieldValue(obj: Any?, fieldName: String): Any? {
        if (obj == null) {
            return null
        }

        var current: Class<*>? = obj.javaClass
        while (current != null) {
            val fields = current.declaredFields
            var index = 0
            while (index < fields.size) {
                val field = fields[index]
                if (field.name == fieldName) {
                    field.isAccessible = true
                    return safeCall { field.get(obj) }
                }
                index += 1
            }
            current = current.superclass
        }

        return null
    }

    private fun readProperty(obj: Any?, methodName: String): Any? {
        if (obj == null) {
            return null
        }

        val method = obj.javaClass.methods.firstOrNull { current ->
            current.name == methodName && current.parameterCount == 0
        } ?: return null

        return safeCall { method.invoke(obj) }
    }

    private fun applyHeaderOrder(table: JTable, headers: List<String>): Boolean {
        val columnModel = table.columnModel
        val count = columnModel.columnCount

        if (headers.size != count) {
            return false
        }

        var anyMoved = false
        var targetIndex = 0
        while (targetIndex < headers.size) {
            val expectedHeader = headers[targetIndex]
            val currentIndex = findColumnIndexByHeader(table, expectedHeader, targetIndex)

            if (currentIndex < 0) {
                return false
            }

            if (currentIndex != targetIndex) {
                columnModel.moveColumn(currentIndex, targetIndex)
                anyMoved = true
            }

            targetIndex += 1
        }

        return anyMoved
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

    private fun resetSelectionAndScroll(table: JTable) {
        table.clearSelection()
        table.selectionModel.clearSelection()
        table.columnModel.selectionModel.clearSelection()

        val rowCount = table.rowCount
        val columnCount = table.columnCount

        if (rowCount > 0 && columnCount > 0) {
            table.setRowSelectionAllowed(false)
            table.setColumnSelectionAllowed(false)
            table.changeSelection(0, 0, false, false)
            table.clearSelection()
            table.setRowSelectionAllowed(true)
            table.setColumnSelectionAllowed(true)
        }

        SwingUtilities.invokeLater {
            table.tableHeader?.repaint()
            table.repaint()

            val firstColumnRect = try {
                table.getCellRect(0, 0, true)
            } catch (_: Exception) {
                Rectangle(0, 0, 1, 1)
            }

            table.scrollRectToVisible(Rectangle(firstColumnRect.x, 0, 1, 1))

            val viewport = SwingUtilities.getAncestorOfClass(JViewport::class.java, table) as? JViewport
            if (viewport != null) {
                val point = viewport.viewPosition
                point.x = 0
                viewport.viewPosition = point
            }
        }
    }

    private fun <T> safeCall(block: () -> T): T? {
        return try {
            block()
        } catch (_: Throwable) {
            null
        }
    }
}