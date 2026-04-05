package io.identificator.datagripcolumnsorter.storage

import java.util.WeakHashMap
import javax.swing.JTable
import javax.swing.table.TableModel

object ColumnOrderStorage {
    private data class TableOrderState(
        val model: TableModel,
        val columnSignature: List<String>,
        val originalHeaders: List<String>
    )

    private val originalOrders = WeakHashMap<JTable, TableOrderState>()

    fun saveOriginalOrderIfNeeded(table: JTable) {
        val currentHeaders = readHeaders(table)
        val currentSignature = buildSignature(currentHeaders)
        val currentModel = table.model
        val savedState = originalOrders[table]

        if (savedState == null) {
            originalOrders[table] = TableOrderState(
                model = currentModel,
                columnSignature = currentSignature,
                originalHeaders = currentHeaders
            )
            return
        }

        if (savedState.model !== currentModel || savedState.columnSignature != currentSignature) {
            originalOrders[table] = TableOrderState(
                model = currentModel,
                columnSignature = currentSignature,
                originalHeaders = currentHeaders
            )
        }
    }

    fun getOriginalOrder(table: JTable): List<String>? {
        return originalOrders[table]?.originalHeaders
    }

    fun matchesCurrentResultSet(table: JTable): Boolean {
        val savedState = originalOrders[table] ?: return false
        val currentSignature = buildSignature(readHeaders(table))
        val currentModel = table.model

        return savedState.model === currentModel
                && savedState.columnSignature == currentSignature
    }

    fun hasSavedOrder(table: JTable): Boolean {
        return originalOrders.containsKey(table)
    }

    fun clear(table: JTable) {
        originalOrders.remove(table)
    }

    private fun readHeaders(table: JTable): List<String> {
        val headers = mutableListOf<String>()
        val columnModel = table.columnModel

        var index = 0
        while (index < columnModel.columnCount) {
            headers += columnModel.getColumn(index).headerValue?.toString().orEmpty()
            index += 1
        }

        return headers
    }

    private fun buildSignature(headers: List<String>): List<String> {
        return headers.sorted()
    }
}