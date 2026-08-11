package com.apptimemachine.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.apptimemachine.ui.apps.AppsListScreen
import com.apptimemachine.ui.backup.BackupScreen
import com.apptimemachine.ui.compare.CompareScreen
import com.apptimemachine.ui.dashboard.DashboardScreen
import com.apptimemachine.ui.details.AppDetailsScreen
import com.apptimemachine.ui.reports.ReportsScreen
import com.apptimemachine.ui.search.SearchScreen
import com.apptimemachine.ui.settings.SettingsScreen
import com.apptimemachine.ui.statistics.StatisticsScreen
import com.apptimemachine.ui.timeline.TimelineScreen

/**
 * Part 1.4A Navigation: "No Navigation Drawer. Everything should be
 * reachable using Bottom Navigation and Top App Bar" — exactly four
 * bottom-nav destinations (Dashboard/Timeline/Apps/Settings), with
 * Reports/Details/Search/Compare/Statistics/Backup pushed on top as
 * regular back-stack entries reachable from Dashboard Quick Actions,
 * Settings, or contextual taps (Part 1.4A Navigation Flow).
 */
private sealed class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : TopLevelDestination("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Timeline : TopLevelDestination("timeline", "Timeline", Icons.Default.History)
    data object Apps : TopLevelDestination("apps", "Apps", Icons.Default.Apps)
    data object Settings : TopLevelDestination("settings", "Settings", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    TopLevelDestination.Dashboard,
    TopLevelDestination.Timeline,
    TopLevelDestination.Apps,
    TopLevelDestination.Settings
)

// Shared by the bottom bar AND any in-screen shortcut (e.g. Dashboard's
// Quick Actions) that jumps to a top-level destination. Using plain
// navController.navigate(route) for these (as Dashboard's onOpenTimeline/
// onOpenApps/onOpenSettings previously did) stacks a new back-stack entry
// on every tap instead of reusing/restoring the existing one, so tapping
// the destination's own bottom-nav tab afterward doesn't return you to
// Dashboard — this popUpTo/launchSingleTop/restoreState combo keeps a
// single instance of each top-level destination in the back stack.
private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private const val ROUTE_APP_DETAILS = "app_details/{appId}"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_STATISTICS = "statistics"
private const val ROUTE_COMPARE = "compare"
private const val ROUTE_BACKUP = "backup"
private const val ROUTE_REPORTS = "reports"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Activity-scoped (created once, outside the NavHost's own back-stack
    // entries) so it — and the persistent bar below — survive navigating
    // between Dashboard/Timeline/Apps/Settings and any pushed screen.
    val scanViewModel: GlobalScanViewModel = hiltViewModel()
    val isScanning by scanViewModel.isScanning.collectAsState()
    val resultMessage by scanViewModel.resultMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            snackbarHostState.showSnackbar(it)
            scanViewModel.consumeResultMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            // Scan Now bar only makes sense as a persistent shortcut on the
            // two screens it was asked for — Dashboard and Timeline — not
            // on Apps, Settings, or any pushed screen (Details/Search/etc).
            val showScanBar = currentDestination?.hierarchy?.any {
                it.route == TopLevelDestination.Dashboard.route || it.route == TopLevelDestination.Timeline.route
            } == true

            // A Box (not a Column) so the pill can float ON TOP of the nav
            // bar's own boundary instead of sitting in its own separate
            // strip — half tucked into the nav bar, half over the content,
            // with nothing but the pill's own surface behind it. No extra
            // full-width colored band that could clash with the page
            // scrolled behind it.
            Box(modifier = Modifier.fillMaxWidth()) {
                AppBottomBar(navController)
                if (showScanBar) {
                    GlobalScanBar(
                        isScanning = isScanning,
                        onScan = scanViewModel::runManualScan,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-26).dp)
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(TopLevelDestination.Dashboard.route) {
                DashboardScreen(
                    onOpenTimeline = { navController.navigateToTopLevel(TopLevelDestination.Timeline.route) },
                    onOpenApps = { navController.navigateToTopLevel(TopLevelDestination.Apps.route) },
                    onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.Settings.route) },
                    onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                    onOpenStatistics = { navController.navigate(ROUTE_STATISTICS) },
                    onOpenCompare = { navController.navigate(ROUTE_COMPARE) },
                    onOpenBackup = { navController.navigate(ROUTE_BACKUP) }
                )
            }
            composable(TopLevelDestination.Timeline.route) {
                TimelineScreen(
                    onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                    onOpenAppDetails = { appId -> navController.navigate("app_details/$appId") }
                )
            }
            composable(TopLevelDestination.Apps.route) {
                AppsListScreen(onOpenAppDetails = { appId -> navController.navigate("app_details/$appId") })
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(onOpenReports = { navController.navigate(ROUTE_REPORTS) })
            }
            composable(ROUTE_REPORTS) {
                ReportsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = ROUTE_APP_DETAILS,
                arguments = listOf(navArgument("appId") { type = androidx.navigation.NavType.LongType })
            ) {
                AppDetailsScreen(onBack = { navController.popBackStack() })
            }

            composable(ROUTE_SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAppDetails = { appId -> navController.navigate("app_details/$appId") }
                )
            }
            composable(ROUTE_STATISTICS) { StatisticsScreen() }
            composable(ROUTE_COMPARE) { CompareScreen() }
            composable(ROUTE_BACKUP) { BackupScreen() }
        }
    }
}

/**
 * Persistent "Scan Now" pill — rendered above the bottom nav on Dashboard
 * and Timeline only (see [AppNavigation]'s `showScanBar` check). Tapping
 * it runs a real scan via [GlobalScanViewModel]; the icon spins while
 * scanning and a Snackbar reports what changed once it finishes.
 */
@Composable
private fun GlobalScanBar(isScanning: Boolean, onScan: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "scan_spin_angle"
    )

    Surface(
        modifier = modifier
            .widthIn(min = 190.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = !isScanning, onClick = onScan),
        color = com.apptimemachine.ui.theme.BrandColors.ScanBar,
        shape = RoundedCornerShape(50),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(19.dp)
                        .rotate(if (isScanning) rotation else 0f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (isScanning) "Scanning…" else "Scan Now",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    if (isScanning) "Checking apps…" else "Start a new scan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.width(14.dp))
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail/nested screens (Part 1.4A: bottom nav is
    // for the four top-level destinations only).
    val isTopLevel = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    if (!isTopLevel) return

    NavigationBar {
        bottomNavItems.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigateToTopLevel(destination.route)
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
