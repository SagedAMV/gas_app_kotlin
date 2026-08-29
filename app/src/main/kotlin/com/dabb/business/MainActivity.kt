package com.dabb.business

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable("inventory") {
                InventoryScreen(
                    onNavigateToDispense = { navController.navigate("dispense") },
                    onNavigateToReports = { navController.navigate("reports") }
                )
            }
            composable("dispense") {
                DispenseScreen(
                    onBack = { navController.popBackStack() },
                    onSaleRecorded = { navController.popBackStack() }
                )
            }
            composable("reports") {
                ReportsScreen()
            }
        }
    }
}
