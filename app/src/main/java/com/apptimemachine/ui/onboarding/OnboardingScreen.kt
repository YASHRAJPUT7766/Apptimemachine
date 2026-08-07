package com.apptimemachine.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.apptimemachine.core.monitoring.PermissionHelper
import kotlinx.coroutines.launch

private data class OnboardingFeature(val icon: ImageVector, val title: String, val description: String)

private val features = listOf(
    OnboardingFeature(Icons.Default.History, "App Timeline", "Build a complete history of your apps from now on."),
    OnboardingFeature(Icons.Default.Storage, "Storage History", "Monitor storage growth over time."),
    OnboardingFeature(Icons.Default.BarChart, "Usage Analytics", "Analyze how your apps are used."),
    OnboardingFeature(Icons.Default.Description, "Reports", "Generate detailed reports anytime."),
    OnboardingFeature(Icons.Default.Lock, "Privacy", "Everything is stored locally unless you choose to export or share it.")
)

/**
 * Part 3.4 First Launch Flow: Welcome -> Feature Intro -> Limitations ->
 * Permissions -> Initial Scan -> Complete -> Dashboard. Implemented as a
 * single horizontal pager rather than separate destinations, matching the
 * spec's "Onboarding Resume: resume from the last completed step" via
 * pager state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val pagerState = rememberPagerState(pageCount = { 4 }) // Welcome, Features, Limitations+Permissions, Scan
    val scope = rememberCoroutineScope()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanComplete by viewModel.scanComplete.collectAsState()
    val appsDetected by viewModel.appsDetected.collectAsState()

    LaunchedEffect(scanComplete) {
        if (scanComplete) {
            kotlinx.coroutines.delay(1200)
            onFinished()
        }
    }

    Scaffold { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage(onNext = { scope.launch { pagerState.animateScrollToPage(1) } })
                1 -> FeaturesPage(onNext = { scope.launch { pagerState.animateScrollToPage(2) } })
                2 -> PermissionsPage(onNext = { scope.launch { pagerState.animateScrollToPage(3) } })
                3 -> ScanPage(
                    isScanning = isScanning,
                    scanComplete = scanComplete,
                    appsDetected = appsDetected,
                    onStart = { viewModel.completeSetup() }
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Your App History Starts Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Track your apps from this moment forward",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Get Started") }
    }
}

@Composable
private fun FeaturesPage(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            features.forEach { feature ->
                Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(feature.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

@Composable
private fun PermissionsPage(onNext: () -> Unit) {
    val context = LocalContext.current
    var usageAccessGranted by remember { mutableStateOf(PermissionHelper.hasUsageAccess(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        usageAccessGranted = PermissionHelper.hasUsageAccess(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Before you begin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "App Time Machine cannot reconstruct events that occurred before monitoring started. Some information depends on the permissions you grant.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        PermissionCard(
            title = "Usage Access",
            description = "Needed to measure how long and how often you use each app.",
            granted = usageAccessGranted,
            onRequest = { launcher.launch(PermissionHelper.usageAccessIntent()) }
        )

        Spacer(Modifier.weight(1f))
        Text(
            "No monitoring data is uploaded automatically.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

@Composable
private fun PermissionCard(title: String, description: String, granted: Boolean, onRequest: () -> Unit) {
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (granted) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRequest) { Text("Grant Access") }
            }
        }
    }
}

@Composable
private fun ScanPage(isScanning: Boolean, scanComplete: Boolean, appsDetected: Int, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            scanComplete -> {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Monitoring Started", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("$appsDetected applications detected", style = MaterialTheme.typography.bodyMedium)
            }
            isScanning -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Reading installed applications…", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text("Ready to start monitoring", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start Monitoring") }
            }
        }
    }
}
