package com.dabb.business

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.ui.screens.PinGate
import com.dabb.business.ui.screens.ReportsScreen
import com.dabb.business.ui.screens.SalesHistoryScreen
import com.dabb.business.ui.screens.SettingsScreen
import com.dabb.business.ui.screens.StationScreen
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
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
import com.dabb.business.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    /** إصلاح المشكلة 16: كل تفاعل مع الشاشة يحدّث وقت آخر نشاط. */
    override fun onUserInteraction() {
        super.onUserInteraction()
        lastInteractionAt = SystemClock.elapsedRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                var unlocked by remember { mutableStateOf(false) }

                if (unlocked) {
                    // ① عند العودة من الخلفية: إن مضت فترة الخمول → إعادة القفل فوراً
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME &&
                                SystemClock.elapsedRealtime() - lastInteractionAt > AUTO_LOCK_MS
                            ) {
                                unlocked = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    // ② والتطبيق مفتوح: فحص دوري كل 30 ثانية
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(CHECK_INTERVAL_MS)
                            if (SystemClock.elapsedRealtime() - lastInteractionAt > AUTO_LOCK_MS) {
                                unlocked = false
                                break
                            }
                        }
                    }
                }

                if (unlocked) AppNavigation()
                else PinGate(onUnlocked = {
                    lastInteractionAt = SystemClock.elapsedRealtime()
                    unlocked = true
                })
            }
        }
    }

    companion object {
        private const val AUTO_LOCK_MS = 5 * 60 * 1000L   // 5 دقائق خمول
        private const val CHECK_INTERVAL_MS = 30 * 1000L   // فحص كل 30 ثانية

        @Volatile
        private var lastInteractionAt = SystemClock.elapsedRealtime()
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
            // إصلاح الفحص M1: قناة أخطاء الخلفية (errorMessage) كانت ميتة —
            // أي فشل غير متوقع (تحديث/خلفية) يظهر هنا ويُغلق تلقائياً بعد 5 ثوانٍ.
            val errorViewModel: AppViewModel = viewModel()
            Box {
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
                GlobalErrorBanner(
                    error = errorViewModel.errorMessage,
                    onDismiss = { errorViewModel.clearError() }
                )
            }
        }
    }
}

/** شريط خطأ عام للأخطاء غير المباشرة — يظهر أعلى الشاشة ويختفي بعد 5 ثوانٍ. */
@Composable
private fun GlobalErrorBanner(error: String?, onDismiss: () -> Unit) {
    LaunchedEffect(error) {
        if (error != null) {
            delay(5000)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut()
    ) {
        error?.let { msg ->
            Row(
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(msg, color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        }
    }
}
