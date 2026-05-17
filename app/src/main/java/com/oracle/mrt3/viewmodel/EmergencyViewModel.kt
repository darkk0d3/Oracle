package com.oracle.mrt3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oracle.mrt3.data.model.EmergencyReport
import com.oracle.mrt3.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SubmitState {
    object Idle    : SubmitState()
    object Loading : SubmitState()
    object Success : SubmitState()
    data class Error(val message: String) : SubmitState()
}

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<EmergencyReport>>(emptyList())
    val reports: StateFlow<List<EmergencyReport>> = _reports.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    // Form fields exposed as MutableStateFlow so composable can bind directly
    val selectedType    = MutableStateFlow("")
    val selectedStation = MutableStateFlow("")
    val description     = MutableStateFlow("")
    val hasPhoto        = MutableStateFlow(false)

    init {
        observeReports()
    }

    private fun observeReports() = viewModelScope.launch {
        repository.getEmergencyReports().collect { _reports.value = it }
    }

    fun submitReport() {
        if (selectedType.value.isEmpty() || selectedStation.value.isEmpty()) {
            _submitState.value = SubmitState.Error("Please select a type and station")
            return
        }
        _submitState.value = SubmitState.Loading
        viewModelScope.launch {
            try {
                repository.submitEmergencyReport(
                    type        = selectedType.value,
                    station     = selectedStation.value,
                    description = description.value,
                    hasPhoto    = hasPhoto.value
                )
                _submitState.value = SubmitState.Success
                clearForm()
            } catch (e: Exception) {
                _submitState.value = SubmitState.Error(e.message ?: "Submission failed")
            }
        }
    }

    private fun clearForm() {
        selectedType.value    = ""
        selectedStation.value = ""
        description.value     = ""
        hasPhoto.value        = false
    }

    fun resetSubmitState() { _submitState.value = SubmitState.Idle }
}
