package com.apptimemachine.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.apptimemachine.ui.notifications.NotificationsScreen
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
private const val ROUTE_NOTIFICATIONS = "notifications"

@Composable
fun AppNavigation(openTimelineOnStart: Boolean = false) {
    val navController = rememberNavController()

    // Deep-link from a tapped event notification (see AppNotificationHelper)
    // — jumps straight to Timeline once the NavHost exists, instead of
    // landing on Dashboard and making the person tap Timeline themselves.
    LaunchedEffect(openTimelineOnStart) {
        if (openTimelineOnStart) {
            navController.navigateToTopLevel(TopLevelDestination.Timeline.route)
        }
    }

    // The app updates live: install/uninstall/permission/storage changes are
    // detected the instant they happen (PackageChangeReceiver + periodic
    // background worker) and written straight to Room, so every screen
    // observing that data via Flow (Dashboard, Timeline, Apps) recomposes
    // automatically. There is no manual "Scan Now" action and no refresh
    // step anywhere in the UI.
    Scaffold(
        bottomBar = { AppBottomBar(navController) }
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
                    onOpenBackup = { navController.navigate(ROUTE_BACKUP) },
                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) }
                )
            }
            composable(TopLevelDestination.Timeline.route) {
                TimelineScreen(
                    onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                    onOpenAppDetails = { appId -> navController.navigate("app_details/$appId") },
                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) }
                )
            }
            composable(TopLevelDestination.Apps.route) {
                AppsListScreen(onOpenAppDetails = { appId -> navController.navigate("app_details/$appId") })
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(
                    onOpenReports = { navController.navigate(ROUTE_REPORTS) },
                    onOpenNotifications = { navController.navigate(ROUTE_NOTIFICATIONS) }
                )
            }
            composable(ROUTE_REPORTS) {
                ReportsScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
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
