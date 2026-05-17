package com.oracle.mrt3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oracle.mrt3.data.model.PersonalContact
import com.oracle.mrt3.data.model.UserProfile
import com.oracle.mrt3.data.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Idle    : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val message: String) : ProfileUiState()
    data class Error(val message: String)   : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _contacts = MutableStateFlow<List<PersonalContact>>(emptyList())
    val contacts: StateFlow<List<PersonalContact>> = _contacts.asStateFlow()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating.asStateFlow()

    private val _feedbackComment = MutableStateFlow("")
    val feedbackComment: StateFlow<String> = _feedbackComment.asStateFlow()

    init {
        loadProfile()
        observeContacts()
    }

    fun loadProfile() = viewModelScope.launch {
        _profile.value = repository.getProfile()
    }

    fun updateDisplayName(name: String) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                repository.updateProfile(name)
                _profile.update { it?.copy(displayName = name) ?: UserProfile(displayName = name) }
                _uiState.value = ProfileUiState.Success("Name updated")
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Update failed")
            }
        }
    }

    fun setRating(stars: Int) { _rating.value = stars }
    fun setFeedbackComment(text: String) { _feedbackComment.value = text }

    fun submitFeedback() {
        val r = _rating.value
        if (r == 0) return
        viewModelScope.launch {
            try {
                repository.submitFeedback(r, _feedbackComment.value)
                _uiState.value = ProfileUiState.Success("Feedback submitted, thank you!")
                _rating.value = 0
                _feedbackComment.value = ""
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Submission failed")
            }
        }
    }

    private fun observeContacts() = viewModelScope.launch {
        repository.getContacts().collect { _contacts.value = it }
    }

    fun addContact(name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addContact(name, phone)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to add contact")
            }
        }
    }

    fun deleteContact(id: String) = viewModelScope.launch {
        try {
            repository.deleteContact(id)
        } catch (e: Exception) {
            _uiState.value = ProfileUiState.Error(e.message ?: "Failed to delete")
        }
    }

    fun resetUiState() { _uiState.value = ProfileUiState.Idle }
}
