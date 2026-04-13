package io.identificator.datagripcolumnsorter.storage

import io.identificator.datagripcolumnsorter.table.TransposedRowHeaderReflectionUtils
import java.util.WeakHashMap
import javax.swing.JTable
import javax.swing.table.TableModel

object TransposedColumnOrderStorage {
    private data class TransposedOrderState(
        val model: TableModel,
        val headerSignature: List<String>,
        val originalHeaders: List<String>
    )

    private val originalOrders = WeakHashMap<JTable, TransposedOrderState>()

    fun saveOriginalOrderIfNeeded(table: JTable) {
        val currentHeaders = TransposedRowHeaderReflectionUtils.readVisibleFieldHeaders(table)
        if (currentHeaders.isEmpty()) {
            return
        }

        val currentSignature = buildSignature(currentHeaders)
        val currentModel = table.model
        val savedState = originalOrders[table]

        if (savedState == null) {
            originalOrders[table] = TransposedOrderState(
                model = currentModel,
                headerSignature = currentSignature,
                originalHeaders = currentHeaders
            )
            return
        }

        if (savedState.model !== currentModel || savedState.headerSignature != currentSignature) {
            originalOrders[table] = TransposedOrderState(
                model = currentModel,
                headerSignature = currentSignature,
                originalHeaders = currentHeaders
            )
        }
    }

    fun getOriginalOrder(table: JTable): List<String>? {
        return originalOrders[table]?.originalHeaders
    }

    fun hasSavedOrder(table: JTable): Boolean {
        return originalOrders.containsKey(table)
    }

    fun matchesCurrentResultSet(table: JTable): Boolean {
        val savedState = originalOrders[table] ?: return false
        val currentHeaders = TransposedRowHeaderReflectionUtils.readVisibleFieldHeaders(table)
        if (currentHeaders.isEmpty()) {
            return false
        }

        val currentSignature = buildSignature(currentHeaders)
        val currentModel = table.model

        return savedState.model === currentModel &&
                savedState.headerSignature == currentSignature
    }

    fun clear(table: JTable) {
        originalOrders.remove(table)
    }

    private fun buildSignature(headers: List<String>): List<String> {
        return headers.sorted()
    }
}