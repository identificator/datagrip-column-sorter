package io.identificator.datagripcolumnsorter.table

import java.util.Locale
import javax.swing.JTable

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

        val sortedHeaders = headersWithIndex
            .sortedWith(compareBy({ it.second.lowercase(Locale.getDefault()) }, { it.first }))
            .map { it.second }

        return applyHeaderOrder(table, sortedHeaders)
    }

    fun restoreOriginalOrder(table: JTable, originalHeaders: List<String>): Boolean {
        if (originalHeaders.isEmpty()) {
            return false
        }

        return applyHeaderOrder(table, originalHeaders)
    }

    private fun applyHeaderOrder(table: JTable, headers: List<String>): Boolean {
        val columnModel = table.columnModel
        val count = columnModel.columnCount

        if (headers.size != count) {
            return false
        }

        var targetIndex = 0
        while (targetIndex < headers.size) {
            val expectedHeader = headers[targetIndex]
            val currentIndex = findColumnIndexByHeader(table, expectedHeader, targetIndex)

            if (currentIndex < 0) {
                return false
            }

            if (currentIndex != targetIndex) {
                columnModel.moveColumn(currentIndex, targetIndex)
            }

            targetIndex += 1
        }

        return true
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

    fun isAlphabeticallySorted(table: JTable): Boolean {
        val columnModel = table.columnModel
        val headers = mutableListOf<String>()

        var index = 0
        while (index < columnModel.columnCount) {
            headers += columnModel.getColumn(index).headerValue?.toString().orEmpty()
            index += 1
        }

        val sortedHeaders = headers
            .mapIndexed { originalIndex, header -> originalIndex to header }
            .sortedWith(compareBy({ it.second.lowercase() }, { it.first }))
            .map { it.second }

        return headers == sortedHeaders
    }
}