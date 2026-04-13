package io.identificator.datagripcolumnsorter.table

import io.identificator.datagripcolumnsorter.settings.ColumnSorterSettingsState
import java.util.Collections
import java.util.Locale
import javax.swing.JTable
import javax.swing.RowSorter
import javax.swing.table.TableModel

object TransposedTableReorderer {
    fun readCurrentOrder(table: JTable): List<String> {
        clearInvalidSorterIfNeeded(table)
        return TransposedRowHeaderReflectionUtils.readVisibleFieldHeaders(table)
    }

    fun isAlphabeticallySorted(table: JTable): Boolean {
        clearInvalidSorterIfNeeded(table)

        val currentHeaders = readCurrentOrder(table)
        if (currentHeaders.size < 2) {
            return true
        }

        val settings = ColumnSorterSettingsState.getInstance().state
        val targetHeaders = buildTargetOrder(currentHeaders, settings)

        return currentHeaders == targetHeaders
    }

    fun buildTargetOrder(
        headers: List<String>,
        settings: ColumnSorterSettingsState.State
    ): List<String> {
        val headersWithIndex = headers.mapIndexed { index, header -> index to header }

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

    fun sortAlphabetically(table: JTable): Boolean {
        clearInvalidSorterIfNeeded(table)

        val currentHeaders = readCurrentOrder(table)
        if (currentHeaders.size < 2) {
            return false
        }

        val settings = ColumnSorterSettingsState.getInstance().state
        val targetHeaders = buildTargetOrder(currentHeaders, settings)

        if (currentHeaders == targetHeaders) {
            return false
        }

        return applyRowOrder(table, currentHeaders, targetHeaders)
    }

    fun restoreOriginalOrder(table: JTable, originalHeaders: List<String>): Boolean {
        clearInvalidSorterIfNeeded(table)

        if (originalHeaders.isEmpty()) {
            return false
        }

        val currentHeaders = readCurrentOrder(table)
        if (currentHeaders.size != originalHeaders.size) {
            return false
        }

        return applyRowOrder(table, currentHeaders, originalHeaders)
    }

    /**
     * Clears a stale row sorter left on a reused JTable instance.
     *
     * DataGrip can reuse the same table component across multiple query executions
     * in one console. In transposed mode the plugin installs a custom RowSorter
     * that stores a fixed row mapping. Once the underlying result set changes,
     * that mapping becomes invalid and must be removed before reading headers
     * or applying a new order.
     */
    private fun clearInvalidSorterIfNeeded(table: JTable) {
        val sorter = table.rowSorter ?: return
        val fixedSorter = sorter as? FixedRowOrderSorter ?: return

        if (!fixedSorter.isCompatibleWith(table.model)) {
            table.rowSorter = null
            table.revalidate()
            table.repaint()
            table.tableHeader?.revalidate()
            table.tableHeader?.repaint()
        }
    }

    /**
     * Builds a new fixed row mapping for the requested target order.
     *
     * The current visible headers are read in view order. When a sorter is already
     * installed, a header position in the current view is different from the
     * corresponding model row index. The current view index must first be resolved
     * by header lookup and then converted to the underlying model index through
     * JTable row index conversion.
     */
    private fun applyRowOrder(
        table: JTable,
        currentHeaders: List<String>,
        targetHeaders: List<String>
    ): Boolean {
        if (currentHeaders.size != targetHeaders.size) {
            return false
        }

        val usedViewIndexes = mutableSetOf<Int>()
        val viewToModel = IntArray(targetHeaders.size)

        var viewIndex = 0
        while (viewIndex < targetHeaders.size) {
            val targetHeader = targetHeaders[viewIndex]
            val currentViewIndex = findHeaderIndex(currentHeaders, targetHeader, usedViewIndexes)
            if (currentViewIndex < 0) {
                return false
            }

            val currentModelIndex = table.convertRowIndexToModel(currentViewIndex)
            if (currentModelIndex < 0) {
                return false
            }

            viewToModel[viewIndex] = currentModelIndex
            usedViewIndexes += currentViewIndex
            viewIndex += 1
        }

        val rowSorter = FixedRowOrderSorter(table.model, viewToModel)
        table.rowSorter = rowSorter
        table.revalidate()
        table.repaint()
        table.tableHeader?.revalidate()
        table.tableHeader?.repaint()

        return true
    }

    private fun findHeaderIndex(
        headers: List<String>,
        expectedHeader: String,
        usedIndexes: Set<Int>
    ): Int {
        var index = 0
        while (index < headers.size) {
            if (index !in usedIndexes && headers[index] == expectedHeader) {
                return index
            }
            index += 1
        }
        return -1
    }

    /**
     * Stores a fixed row mapping for transposed result views.
     *
     * The sorter reorders logical rows in the transposed table so that original
     * column names can be displayed in the requested order.
     */
    private class FixedRowOrderSorter(
        private val tableModel: TableModel,
        private val viewToModelRows: IntArray
    ) : RowSorter<TableModel>() {

        private val modelToViewRows: IntArray = IntArray(viewToModelRows.size) { -1 }
        private var valid = true

        init {
            rebuildReverseIndex()
        }

        private fun rebuildReverseIndex() {
            var index = 0
            while (index < modelToViewRows.size) {
                modelToViewRows[index] = -1
                index += 1
            }

            var viewIndex = 0
            while (viewIndex < viewToModelRows.size) {
                val modelIndex = viewToModelRows[viewIndex]
                if (modelIndex >= 0 && modelIndex < modelToViewRows.size) {
                    modelToViewRows[modelIndex] = viewIndex
                }
                viewIndex += 1
            }
        }

        /**
         * Marks the stored mapping as invalid after model updates.
         *
         * After invalidation, the sorter falls back to identity index conversion
         * to avoid applying stale row indices to a different result set.
         */
        private fun invalidateSorter() {
            if (!valid) {
                return
            }

            valid = false
            fireRowSorterChanged(null)
        }

        override fun getModel(): TableModel {
            return tableModel
        }

        override fun getModelRowCount(): Int {
            return tableModel.rowCount
        }

        override fun getViewRowCount(): Int {
            if (!valid) {
                return tableModel.rowCount
            }
            return viewToModelRows.size
        }

        override fun convertRowIndexToModel(index: Int): Int {
            if (!valid) {
                if (index < 0 || index >= tableModel.rowCount) {
                    return -1
                }
                return index
            }

            if (index < 0 || index >= viewToModelRows.size) {
                return -1
            }

            return viewToModelRows[index]
        }

        override fun convertRowIndexToView(index: Int): Int {
            if (!valid) {
                if (index < 0 || index >= tableModel.rowCount) {
                    return -1
                }
                return index
            }

            if (index < 0 || index >= modelToViewRows.size) {
                return -1
            }

            return modelToViewRows[index]
        }

        /**
         * Checks whether the sorter still matches the active table model.
         *
         * The sorter is stale if the model instance changed or if the current
         * row count no longer matches the stored mapping size.
         */
        fun isCompatibleWith(currentModel: TableModel): Boolean {
            if (currentModel !== tableModel) {
                return false
            }

            if (currentModel.rowCount != viewToModelRows.size) {
                return false
            }

            return true
        }

        override fun toggleSortOrder(column: Int) {
        }

        override fun getSortKeys(): MutableList<out SortKey> {
            return Collections.emptyList<SortKey>()
        }

        override fun setSortKeys(keys: MutableList<out SortKey>?) {
        }

        override fun modelStructureChanged() {
            invalidateSorter()
        }

        override fun allRowsChanged() {
            invalidateSorter()
        }

        override fun rowsInserted(firstRow: Int, endRow: Int) {
            invalidateSorter()
        }

        override fun rowsDeleted(firstRow: Int, endRow: Int) {
            invalidateSorter()
        }

        override fun rowsUpdated(firstRow: Int, endRow: Int) {
            invalidateSorter()
        }

        override fun rowsUpdated(firstRow: Int, endRow: Int, column: Int) {
            invalidateSorter()
        }
    }
}