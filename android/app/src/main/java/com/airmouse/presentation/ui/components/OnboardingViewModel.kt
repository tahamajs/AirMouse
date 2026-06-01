package com.airmouse.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val uiState: StateFlow<Int> = _currentPage.asStateFlow()

    fun nextPage() {
        _currentPage.value++
    }

    fun previousPage() {
        if (_currentPage.value > 0) _currentPage.value--
    }
}