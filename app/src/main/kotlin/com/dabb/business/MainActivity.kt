package com.dabb.business

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.dabb.business.ui.animation.animatedComposable
import com.dabb.business.ui.screens.DispenseScreen
import com.dabb.business.ui.screens.InventoryScreen
import com.dabb.business.ui.screens.ReportsScreen
import com.dabb.business.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "inventory",
            modifier = Modifier.padding(padding)
        ) {
            // انتقالات الشاشات: انزلاق من الأسفل + تلاشي (المعرض #75 / #76)
            animatedComposable("inventory") {
                InventoryScreen(
                    onNavigateToDispense = { navController.navigate("dispense") },
                    onNavigateToReports = { navController.navigate("reports") }
                )
            }
            animatedComposable("dispense") {
                DispenseScreen(
                    onBack = { navController.popBackStack() },
                    onSaleRecorded = { navController.popBackStack() }
                )
            }
            animatedComposable("reports") {
                ReportsScreen()
            }
        }
    }
}
