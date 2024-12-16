package com.despicable.feature.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.despicable.domain.usecase.widget.WidgetUseCases
import com.despicable.domain.usecase.widget.model.WidgetOperationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class WidgetViewModel @Inject constructor(
    private val widgetUseCases: WidgetUseCases
) : ViewModel() {

    private val _widgetState = MutableStateFlow<WidgetOperationResult<Unit>>(WidgetOperationResult.Loading)
    val widgetState: StateFlow<WidgetOperationResult<Unit>> = _widgetState

    fun saveWidgetNote(widgetId: Int, noteId: Long) {
        viewModelScope.launch {
            _widgetState.value = WidgetOperationResult.Loading
            try {
                val result = widgetUseCases.saveWidgetNote(widgetId, noteId)
                _widgetState.value = when (result) {
                    is com.despicable.widgets.data.NoteWidgetRepository.WidgetResult.Success ->
                        WidgetOperationResult.Success(Unit)
                    is com.despicable.widgets.data.NoteWidgetRepository.WidgetResult.Error ->
                        WidgetOperationResult.Error(result.exception)
                }
            } catch (e: Exception) {
                _widgetState.value = WidgetOperationResult.Error(e)
            }
        }
    }

    fun restoreWidgets() {
        viewModelScope.launch {
            _widgetState.value = WidgetOperationResult.Loading
            try {
                val result = widgetUseCases.restoreWidgets()
                _widgetState.value = when (result) {
                    is com.despicable.widgets.data.NoteWidgetRepository.WidgetResult.Success ->
                        WidgetOperationResult.Success(Unit)
                    is com.despicable.widgets.data.NoteWidgetRepository.WidgetResult.Error ->
                        WidgetOperationResult.Error(result.exception)
                }
            } catch (e: Exception) {
                _widgetState.value = WidgetOperationResult.Error(e)
            }
        }
    }
} 