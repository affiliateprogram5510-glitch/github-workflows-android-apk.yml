package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.PowerForgeViewModel
import com.example.ui.TelemetryUiState
import com.example.ui.screens.DeployGuideScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SystemInspectorScreen
import com.example.ui.theme.PowerForgeTheme
import kotlinx.coroutines.launch

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_SYSTEM = "system"
const val ROUTE_DEPLOY = "deploy"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topTabs = listOf(
    NavTab(
        route = ROUTE_OVERVIEW,
        labelRes = R.string.nav_overview,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    NavTab(
        route = ROUTE_SYSTEM,
        labelRes = R.string.nav_inspector,
        selectedIcon = Icons.Filled.VerifiedUser,
        unselectedIcon = Icons.Outlined.VerifiedUser
    ),
    NavTab(
        route = ROUTE_DEPLOY,
        labelRes = R.string.nav_deploy,
        selectedIcon = Icons.Filled.CloudDownload,
        unselectedIcon = Icons.Outlined.CloudDownload
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PowerForgeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val dynamicColor = (uiState as? TelemetryUiState.Success)?.useDynamicColor ?: false

            PowerForgeTheme(dynamicColor = dynamicColor) {
                PowerForgeApp(
                    uiState = uiState,
                    onRefresh = viewModel::refreshTelemetry,
                    onSendTestNotification = viewModel::sendDiagnosticNotification,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    onToggleDeployStep = viewModel::toggleDeployStep,
                    onResetChecklist = viewModel::resetDeployChecklist
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerForgeApp(
    uiState: TelemetryUiState,
    onRefresh: () -> Unit,
    onSendTestNotification: () -> Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    onToggleDeployStep: (String) -> Unit,
    onResetChecklist: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_OVERVIEW
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Mobile Build & Realme Companion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .widthIn(max = 840.dp)
                    ) {
                        when (uiState) {
                            is TelemetryUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is TelemetryUiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(48.dp))
                                    Text(
                                        text = uiState.message,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onRefresh) {
                                        Text(stringResource(R.string.action_refresh))
                                    }
                                }
                            }

                            is TelemetryUiState.Success -> {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_OVERVIEW
                                ) {
                                    composable(ROUTE_OVERVIEW) {
                                        OverviewScreen(
                                            state = uiState,
                                            onRefresh = onRefresh
                                        )
                                    }
                                    composable(ROUTE_SYSTEM) {
                                        BackHandler { navigateToTab(ROUTE_OVERVIEW) }
                                        SystemInspectorScreen(
                                            state = uiState,
                                            onRefreshStatus = onRefresh,
                                            onSendTestNotification = onSendTestNotification,
                                            onDynamicColorChange = onDynamicColorChange,
                                            onShowMessage = { msg ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        )
                                    }
                                    composable(ROUTE_DEPLOY) {
                                        BackHandler { navigateToTab(ROUTE_OVERVIEW) }
                                        DeployGuideScreen(
                                            completedStepIds = uiState.completedStepIds,
                                            onToggleStep = onToggleDeployStep,
                                            onResetChecklist = onResetChecklist
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
