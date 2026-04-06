package io.identificator.datagripcolumnsorter.table

import io.identificator.datagripcolumnsorter.settings.ColumnSorterSettingsState
import java.awt.Rectangle
import java.util.Locale
import javax.swing.JTable
import javax.swing.JViewport
import javax.swing.SwingUtilities

object TableColumnReorderer {
    fun sortAlphabetically(table: JTable): Boolean {
        val columnModel = table.columnModel
        val count = columnModel.columnCount

        if (count < 2) {
            return false
        }

        val headersWithIndex = mutableListOf<Pair<Int, String>>()

        var index = 0
        while (index < count) {
            val header = columnModel.getColumn(index).headerValue?.toString().orEmpty()
            headersWithIndex += index to header
            index += 1
        }

        val settings = ColumnSorterSettingsState.getInstance().state
        val targetHeaders = buildTargetOrder(headersWithIndex, settings)
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
        val columnModel = table.columnModel
        val count = columnModel.columnCount
        val headersWithIndex = mutableListOf<Pair<Int, String>>()

        var index = 0
        while (index < count) {
            val header = columnModel.getColumn(index).headerValue?.toString().orEmpty()
            headersWithIndex += index to header
            index += 1
        }

        val settings = ColumnSorterSettingsState.getInstance().state
        val targetHeaders = buildTargetOrder(headersWithIndex, settings)
        val currentHeaders = headersWithIndex.map { it.second }

        return currentHeaders == targetHeaders
    }

    private fun buildTargetOrder(
        headersWithIndex: List<Pair<Int, String>>,
        settings: ColumnSorterSettingsState.State
    ): List<String> {
        if (!settings.enablePinnedColumnsFirst) {
            return headersWithIndex
                .sortedWith(compareBy({ it.second.lowercase(Locale.getDefault()) }, { it.first }))
                .map { it.second }
        }

        val pinnedNames = settings.pinnedColumnNames
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
            .toSet()

        val pinned = mutableListOf<Pair<Int, String>>()
        val regular = mutableListOf<Pair<Int, String>>()

        headersWithIndex.forEach { pair ->
            val headerLower = pair.second.trim().lowercase(Locale.getDefault())
            if (headerLower in pinnedNames) {
                pinned += pair
            } else {
                regular += pair
            }
        }

        val sortedRegular = regular
            .sortedWith(compareBy({ it.second.lowercase(Locale.getDefault()) }, { it.first }))

        return (pinned + sortedRegular).map { it.second }
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
}