package com.apptimemachine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * handled by the system splash screen API (see themes.xml, Theme.AppTimeMachine.Splash) —
 * installSplashScreen() here keeps the branded splash on-screen until the
 * onboarding-completed flag has actually been read from DataStore, so we
 * never flash an unbranded blank screen or a bare loading spinner between
 * the splash and the first real screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** Set on the tap intent by AppNotificationHelper for event alerts. */
        const val EXTRA_OPEN_TIMELINE = "open_timeline"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        val openTimeline = intent?.getBooleanExtra(EXTRA_OPEN_TIMELINE, false) ?: false

        enableEdgeToEdge()
        setContent {
            // Ask for POST_NOTIFICATIONS (Android 13+) the moment the app
            // opens, so real-time install/update/removal alerts can
            // actually be delivered. On API < 33 this permission is
            // granted at install time automatically, so nothing to do.
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* no-op: MonitoringManager checks the live permission state before every notify */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            RootContent(onReady = { isReady = true }, openTimelineOnStart = openTimeline)
        }
    }
}

@Composable
private fun RootContent(
    onReady: () -> Unit,
    openTimelineOnStart: Boolean = false,
    viewModel: RootViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsState(initial = AppTheme.SYSTEM)
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = false)
    val amoledMode by viewModel.amoledMode.collectAsState(initial = false)
    val onboardingState by viewModel.onboardingCompleted.collectAsState(initial = null)

    androidx.compose.runtime.LaunchedEffect(onboardingState) {
        if (onboardingState != null) onReady()
    }

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
                true -> AppNavigation(openTimelineOnStart = openTimelineOnStart)
            }
        }
    }
}
