package io.identificator.datagripcolumnsorter.debug

import java.lang.reflect.Method
import javax.swing.JTable

object GridColumnDebugUtils {
    private val interestingKeywords = listOf(
        "type",
        "types",
        "data",
        "meta",
        "metadata",
        "jdbc",
        "sql",
        "native",
        "spec",
        "field",
        "column",
        "attribute",
        "class"
    )

    fun dumpGridColumnInfo(gridColumn: Any?) {
        dumpObject("gridColumn", gridColumn)
        invokeInterestingNoArgMethods("gridColumn", gridColumn)
    }

    fun dumpModelInfo(model: Any?) {
        dumpObject("model", model)
        invokeInterestingNoArgMethods("model", model)
    }

    private fun dumpObject(label: String, obj: Any?) {
        println("=== $label ===")

        if (obj == null) {
            println("null")
            return
        }

        val clazz = obj.javaClass
        println("class: ${clazz.name}")

        val fields = collectFields(clazz)
        var printedField = false
        var fieldIndex = 0

        while (fieldIndex < fields.size) {
            val field = fields[fieldIndex]
            val fieldNameLower = field.name.lowercase()

            if (containsInterestingKeyword(fieldNameLower)) {
                printedField = true
                try {
                    field.isAccessible = true
                    val value = field.get(obj)
                    val valueClass = value?.javaClass?.name ?: "null"
                    println("field: ${field.name} [$valueClass] = $value")
                } catch (_: Throwable) {
                    println("field: ${field.name} = <inaccessible>")
                }
            }

            fieldIndex += 1
        }

        if (!printedField) {
            println("interesting fields: <none>")
        }

        val methods = collectMethods(clazz)
            .filter { method -> containsInterestingKeyword(method.name.lowercase()) }
            .map { method -> buildMethodSignature(method) }
            .distinct()
            .sorted()

        if (methods.isEmpty()) {
            println("interesting methods: <none>")
            return
        }

        println("interesting methods:")
        var methodIndex = 0
        while (methodIndex < methods.size) {
            println("  ${methods[methodIndex]}")
            methodIndex += 1
        }
    }

    private fun invokeInterestingNoArgMethods(label: String, obj: Any?) {
        println("=== $label no-arg method results ===")

        if (obj == null) {
            println("null")
            return
        }

        val methods = collectMethods(obj.javaClass)
            .filter { method ->
                method.parameterCount == 0 &&
                        containsInterestingKeyword(method.name.lowercase()) &&
                        !isIgnoredMethodName(method.name)
            }
            .sortedBy { it.name }

        if (methods.isEmpty()) {
            println("interesting no-arg methods: <none>")
            return
        }

        var index = 0
        while (index < methods.size) {
            val method = methods[index]

            try {
                method.isAccessible = true
                val value = method.invoke(obj)
                val valueClass = value?.javaClass?.name ?: "null"
                println("${method.name}() [$valueClass] = $value")
            } catch (_: Throwable) {
                println("${method.name}() = <failed>")
            }

            index += 1
        }
    }

    private fun collectFields(clazz: Class<*>): List<java.lang.reflect.Field> {
        val result = mutableListOf<java.lang.reflect.Field>()
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

    private fun isIgnoredMethodName(name: String): Boolean {
        return name == "getClass" ||
                name == "hashCode" ||
                name == "toString" ||
                name == "clone" ||
                name == "notify" ||
                name == "notifyAll"
    }

    fun dumpAllColumnTypes(table: JTable?) {
        println("=== ALL COLUMN TYPES START ===")

        if (table == null) {
            println("table = null")
            println("=== ALL COLUMN TYPES END ===")
            return
        }

        val model = table.model
        val createColumnMethod = model.javaClass.methods.firstOrNull { current ->
            current.name == "createColumn" &&
                    current.parameterCount == 1 &&
                    current.parameterTypes[0] == Int::class.javaPrimitiveType
        }

        if (createColumnMethod == null) {
            println("createColumn(int) method not found on model: ${model.javaClass.name}")
            println("=== ALL COLUMN TYPES END ===")
            return
        }

        var viewIndex = 0
        while (viewIndex < table.columnCount) {
            try {
                val modelIndex = table.convertColumnIndexToModel(viewIndex)
                val header = table.columnModel.getColumn(viewIndex).headerValue?.toString().orEmpty()

                val tableResultViewColumn = createColumnMethod.invoke(model, modelIndex)
                val rawColumn = readFieldValue(tableResultViewColumn, "myColumn")

                val name = readProperty(rawColumn, "getName")
                val type = readProperty(rawColumn, "getType")
                val typeName = readProperty(rawColumn, "getTypeName")
                val clazz = readProperty(rawColumn, "getClazz")
                val tableRef = readProperty(rawColumn, "getTable")

                println(
                    "viewIndex=$viewIndex, " +
                            "modelIndex=$modelIndex, " +
                            "header='$header', " +
                            "name='$name', " +
                            "type=$type, " +
                            "typeName='$typeName', " +
                            "clazz='$clazz', " +
                            "table='$tableRef'"
                )
            } catch (error: Throwable) {
                println("viewIndex=$viewIndex, error=${error.javaClass.name}: ${error.message}")
            }

            viewIndex += 1
        }

        println("=== ALL COLUMN TYPES END ===")
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
                    return field.get(obj)
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

        return try {
            val method = obj.javaClass.methods.firstOrNull { current ->
                current.name == methodName && current.parameterCount == 0
            } ?: return null

            method.invoke(obj)
        } catch (_: Throwable) {
            null
        }
    }
}