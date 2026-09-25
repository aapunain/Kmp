package com.self.kmp.presentation.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.self.kmp.domain.common.fold
import com.self.kmp.domain.items.model.Item
import com.self.kmp.domain.items.usecase.GetItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the UI state. `androidx.lifecycle.ViewModel` is multiplatform, so this
 * single class serves Android, iOS, desktop and web with no per-platform
 * wrapper.
 *
 * It contains no business rules: filtering and ordering live in
 * [GetItemsUseCase]. What lives here is presentation policy, namely how to turn
 * a domain result into something drawable.
 */
public class ItemsViewModel(
    private val getItems: GetItemsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ItemsUiState())
    public val state: StateFlow<ItemsUiState> = _state.asStateFlow()

    init {
        onIntent(ItemsIntent.Load)
    }

    public fun onIntent(intent: ItemsIntent) {
        when (intent) {
            ItemsIntent.Load,
            ItemsIntent.Retry,
            -> loadItems()
        }
    }

    private fun loadItems() {
        if (_state.value.isLoading) return

        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            getItems().fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = items.map(Item::name),
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = emptyList(),
                            errorMessage = error.toUiMessage(),
                        )
                    }
                },
            )
        }
    }
}
