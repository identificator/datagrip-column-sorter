package io.identificator.datagripcolumnsorter.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "DataGripColumnSorterSettings",
    storages = [Storage("datagrip-column-sorter.xml")]
)
class ColumnSorterSettingsState : PersistentStateComponent<ColumnSorterSettingsState.State> {
    data class TypeSortRule(
        var jdbcType: Int = 12,
        var typeName: String = "varchar"
    )

    data class State(
        var enablePinnedColumnsFirst: Boolean = true,
        var pinnedColumnNames: MutableList<String> = mutableListOf("id", "uuid", "guid", "oid"),
        var showSortButton: Boolean = true,
        var showRestoreButton: Boolean = true,
        var enableSortByTypeFirst: Boolean = false,
        var typeSortRules: MutableList<TypeSortRule> = mutableListOf(
            TypeSortRule(jdbcType = -7, typeName = "bool"),
            TypeSortRule(jdbcType = 4, typeName = "serial"),
            TypeSortRule(jdbcType = 4, typeName = "int4"),
            TypeSortRule(jdbcType = 5, typeName = "int2"),
            TypeSortRule(jdbcType = -5, typeName = "int8"),
            TypeSortRule(jdbcType = 2, typeName = "numeric"),
            TypeSortRule(jdbcType = 3, typeName = "decimal"),
            TypeSortRule(jdbcType = 12, typeName = "varchar"),
            TypeSortRule(jdbcType = 12, typeName = "text"),
            TypeSortRule(jdbcType = 1, typeName = "bpchar"),
            TypeSortRule(jdbcType = 91, typeName = "date"),
            TypeSortRule(jdbcType = 92, typeName = "time"),
            TypeSortRule(jdbcType = 93, typeName = "timestamp"),
            TypeSortRule(jdbcType = 1111, typeName = "jsonb"),
            TypeSortRule(jdbcType = 1111, typeName = "json")
        )
    )

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): ColumnSorterSettingsState {
            return ApplicationManager.getApplication().getService(ColumnSorterSettingsState::class.java)
        }
    }
}