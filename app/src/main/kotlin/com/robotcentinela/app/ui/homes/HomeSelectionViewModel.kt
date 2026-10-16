package com.robotcentinela.app.ui.homes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotcentinela.app.identity.domain.HomeId
import com.robotcentinela.app.identity.domain.HomeMembership
import com.robotcentinela.app.shell.AppContainer
import com.robotcentinela.app.ui.model.OperationPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeSelectionState(
    val loading: Boolean = true,
    val homes: List<HomeMembership> = emptyList(),
    val error: String? = null,
    val selecting: HomeId? = null,
)

/** Selección explícita del domicilio a consultar (RF02, HU01 CDA 2). */
class HomeSelectionViewModel(private val container: AppContainer) : ViewModel() {

    private val mutable = MutableStateFlow(HomeSelectionState())
    val state: StateFlow<HomeSelectionState> = mutable.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutable.value = mutable.value.copy(loading = true, error = null)
        viewModelScope.launch {
            mutable.value = try {
                HomeSelectionState(loading = false, homes = container.listAuthorizedHomes())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                HomeSelectionState(loading = false, error = OperationPresentation.errorMessage(e))
            }
        }
    }

    fun select(homeId: HomeId, onChosen: () -> Unit) {
        if (mutable.value.selecting != null) return
        mutable.value = mutable.value.copy(selecting = homeId, error = null)
        viewModelScope.launch {
            try {
                container.selectHomeScope(homeId)
                mutable.value = mutable.value.copy(selecting = null)
                onChosen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.value = mutable.value.copy(selecting = null, error = OperationPresentation.errorMessage(e))
            }
        }
    }
}
