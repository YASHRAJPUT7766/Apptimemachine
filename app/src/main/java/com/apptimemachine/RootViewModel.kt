package com.apptimemachine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptimemachine.core.datastore.AppTheme
import com.apptimemachine.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives MainActivity's top-level decision: show Onboarding or the main
 * NavHost. onboardingCompleted starts as null (splash/loading state) and
 * resolves to a real Boolean once DataStore has been read.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val theme: Flow<AppTheme> = userPreferences.theme
    val dynamicColor: Flow<Boolean> = userPreferences.dynamicColorEnabled
    val amoledMode: Flow<Boolean> = userPreferences.amoledMode

    private val _onboardingCompleted = MutableStateFlow<Boolean?>(null)
    val onboardingCompleted: StateFlow<Boolean?> = _onboardingCompleted.asStateFlow()

    init {
        refreshOnboardingState()
    }

    fun refreshOnboardingState() {
        viewModelScope.launch {
            _onboardingCompleted.value = userPreferences.onboardingCompleted.first()
        }
    }
}
