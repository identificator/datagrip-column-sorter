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
    data class State(
        var enablePinnedColumnsFirst: Boolean = true,
        var pinnedColumnNames: MutableList<String> = mutableListOf("id", "uuid", "guid", "oid"),
        var showSortButton: Boolean = true,
        var showRestoreButton: Boolean = true
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