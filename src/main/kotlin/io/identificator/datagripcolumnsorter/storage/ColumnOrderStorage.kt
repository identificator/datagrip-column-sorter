package io.identificator.datagripcolumnsorter.storage

import java.util.WeakHashMap
import javax.swing.JTable

object ColumnOrderStorage {
    private val originalOrders = WeakHashMap<JTable, List<String>>()

    fun saveOriginalOrderIfAbsent(table: JTable) {
        if (originalOrders.containsKey(table)) {
            return
        }

        val headers = mutableListOf<String>()
        val columnModel = table.columnModel

        var index = 0
        while (index < columnModel.columnCount) {
            headers += columnModel.getColumn(index).headerValue?.toString().orEmpty()
            index += 1
        }

        originalOrders[table] = headers
    }

    fun getOriginalOrder(table: JTable): List<String>? {
        return originalOrders[table]
    }

    fun clear(table: JTable) {
        originalOrders.remove(table)
    }
}