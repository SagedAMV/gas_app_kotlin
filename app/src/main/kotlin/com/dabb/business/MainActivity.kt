package com.dabb.business

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dabb.business.ui.animation.animatedComposable
import com.dabb.business.ui.screens.CustomerDetailScreen
import com.dabb.business.ui.screens.CustomersScreen
import com.dabb.business.ui.screens.DispenseScreen
import com.dabb.business.ui.screens.InventoryScreen
import com.dabb.business.ui.screens.PinGate
import com.dabb.business.ui.screens.ReportsScreen
import com.dabb.business.ui.screens.SalesHistoryScreen
import com.dabb.business.ui.screens.SettingsScreen
import com.dabb.business.ui.screens.StationScreen
import com.dabb.business.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                var unlocked by remember { mutableStateOf(false) }
                if (unlocked) AppNavigation() else PinGate(onUnlocked = { unlocked = true })
            }
        }
    }
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("inventory", "المخزون", Icons.Filled.Inventory2),
    TabItem("dispense", "الصرف", Icons.Filled.PointOfSale),
    TabItem("customers", "الزبائن", Icons.Filled.People),
    TabItem("station", "المحطة", Icons.Filled.LocalShipping),
    TabItem("reports", "التقارير", Icons.Filled.BarChart)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "inventory",
                modifier = Modifier.padding(padding)
            ) {
                animatedComposable("inventory") {
                    InventoryScreen(
                        onNavigateToDispense = { navController.navigate("dispense") },
                        onNavigateToReports = { navController.navigate("reports") },
                        onNavigateToSales = { navController.navigate("sales_history") }
                    )
                }
                animatedComposable("dispense") { DispenseScreen() }
                animatedComposable("customers") {
                    CustomersScreen(onOpenCustomer = { id -> navController.navigate("customer/$id") })
                }
                animatedComposable("station") { StationScreen() }
                animatedComposable("reports") {
                    ReportsScreen(
                        onOpenCustomer = { id -> navController.navigate("customer/$id") },
                        onOpenCustomers = { navController.navigate("customers") },
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }
                composable("settings") { SettingsScreen() }
                composable("sales_history") { SalesHistoryScreen() }
                composable(
                    route = "customer/{customerId}",
                    arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("customerId").orEmpty()
                    CustomerDetailScreen(customerId = id, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
