package com.apptimemachine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.datastore.AppTheme
import com.apptimemachine.ui.navigation.AppNavigation
import com.apptimemachine.ui.onboarding.OnboardingScreen
import com.apptimemachine.ui.theme.AppTimeMachineTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Part 3.4 Application Startup Flow: Splash -> init -> onboarding (first
 * launch) or resume monitoring -> Dashboard. The splash step itself is
 * handled by the system splash screen API (see themes.xml); this Activity
 * picks Onboarding vs the main NavHost based on persisted state.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RootContent()
        }
    }
}

@Composable
private fun RootContent(viewModel: RootViewModel = hiltViewModel()) {
    val theme by viewModel.theme.collectAsState(initial = AppTheme.SYSTEM)
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = true)
    val amoledMode by viewModel.amoledMode.collectAsState(initial = false)
    val onboardingState by viewModel.onboardingCompleted.collectAsState(initial = null)

    AppTimeMachineTheme(
        darkTheme = when (theme) {
            AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
        },
        dynamicColor = dynamicColor,
        amoledMode = amoledMode
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (onboardingState) {
                null -> androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                false -> OnboardingScreen(onFinished = { viewModel.refreshOnboardingState() })
                true -> AppNavigation()
            }
        }
    }
}
