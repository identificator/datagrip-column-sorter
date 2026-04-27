package io.identificator.datagripcolumnsorter.debug

import java.lang.reflect.Field
import java.lang.reflect.Method
import javax.swing.JTable

object TableReflectionDebugUtils {
    private val interestingKeywords = listOf(
        "type",
        "types",
        "column",
        "columns",
        "meta",
        "metadata",
        "jdbc",
        "sql",
        "field",
        "fields",
        "grid",
        "result",
        "descriptor",
        "schema",
        "model"
    )

    fun dumpTableStructure(table: JTable?) {
        if (table == null) {
            println("=== TABLE DEBUG ===")
            println("table = null")
            return
        }

        println("=== TABLE DEBUG START ===")
        dumpObject("table", table)
        dumpObject("table.model", table.model)
        dumpObject("table.columnModel", table.columnModel)
        dumpObject("table.rowSorter", table.rowSorter)
        dumpObject("table.tableHeader", table.tableHeader)
        dumpObject("table.parent", table.parent)
        dumpObject("table.parent.parent", table.parent?.parent)
        println("=== TABLE DEBUG END ===")
    }

    private fun dumpObject(label: String, obj: Any?) {
        if (obj == null) {
            println("--- $label ---")
            println("null")
            return
        }

        val clazz = obj.javaClass

        println("--- $label ---")
        println("class: ${clazz.name}")

        dumpInterestingFields(clazz, obj)
        dumpInterestingMethods(clazz)
    }

    private fun dumpInterestingFields(clazz: Class<*>, obj: Any) {
        val fields = collectFields(clazz)
        var printedAny = false

        var index = 0
        while (index < fields.size) {
            val field = fields[index]
            val fieldNameLower = field.name.lowercase()

            if (containsInterestingKeyword(fieldNameLower)) {
                printedAny = true
                printFieldValue(field, obj)
            }

            index += 1
        }

        if (!printedAny) {
            println("interesting fields: <none>")
        }
    }

    private fun dumpInterestingMethods(clazz: Class<*>) {
        val methods = collectMethods(clazz)
            .filter { method ->
                containsInterestingKeyword(method.name.lowercase())
            }
            .map { method ->
                buildMethodSignature(method)
            }
            .distinct()
            .sorted()

        if (methods.isEmpty()) {
            println("interesting methods: <none>")
            return
        }

        println("interesting methods:")
        var index = 0
        while (index < methods.size) {
            println("  $methods[index]")
            index += 1
        }
    }

    private fun printFieldValue(field: Field, obj: Any) {
        try {
            field.isAccessible = true
            val value = field.get(obj)
            val valueClass = value?.javaClass?.name ?: "null"
            println("field: ${field.name} [$valueClass] = $value")
        } catch (_: Throwable) {
            println("field: ${field.name} = <inaccessible>")
        }
    }

    private fun collectFields(clazz: Class<*>): List<Field> {
        val result = mutableListOf<Field>()
        var current: Class<*>? = clazz

        while (current != null) {
            val declaredFields = current.declaredFields
            var index = 0
            while (index < declaredFields.size) {
                result += declaredFields[index]
                index += 1
            }
            current = current.superclass
        }

        return result
    }

    private fun collectMethods(clazz: Class<*>): List<Method> {
        val result = mutableListOf<Method>()
        var current: Class<*>? = clazz

        while (current != null) {
            val declaredMethods = current.declaredMethods
            var index = 0
            while (index < declaredMethods.size) {
                result += declaredMethods[index]
                index += 1
            }
            current = current.superclass
        }

        return result
    }

    private fun containsInterestingKeyword(value: String): Boolean {
        return interestingKeywords.any { keyword -> value.contains(keyword) }
    }

    private fun buildMethodSignature(method: Method): String {
        val params = method.parameterTypes.joinToString(", ") { it.simpleName }
        return "${method.name}($params): ${method.returnType.simpleName}"
    }
}