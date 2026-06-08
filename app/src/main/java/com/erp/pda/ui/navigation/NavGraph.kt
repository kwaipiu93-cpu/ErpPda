package com.erp.pda.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.checkout.CheckoutScreen
import com.erp.pda.ui.createpo.CreatePoScreen
import com.erp.pda.ui.createquote.CreateQuoteScreen
import com.erp.pda.ui.customers.CustomerScreen
import com.erp.pda.ui.dispatch.DispatchScreen
import com.erp.pda.ui.home.HomeScreen
import com.erp.pda.ui.invoicelist.InvoiceListScreen
import com.erp.pda.ui.invoicelookup.InvoiceLookupScreen
import com.erp.pda.ui.login.LoginScreen
import com.erp.pda.ui.lookup.LookupScreen
import com.erp.pda.ui.polist.PoListScreen
import com.erp.pda.ui.quotedetail.QuoteDetailScreen
import com.erp.pda.ui.quotelist.QuoteListScreen
import com.erp.pda.ui.receiving.ReceivingScreen
import com.erp.pda.ui.recordpayment.RecordPaymentScreen
import com.erp.pda.ui.returns.ReturnScreen
import com.erp.pda.ui.stockcheck.StockCheckScreen
import com.erp.pda.ui.stocktake.StocktakeScreen
import com.erp.pda.ui.transfer.TransferScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"

    // Tab roots
    const val TAB_HOME = "tab_home"
    const val TAB_PURCHASE = "tab_purchase"
    const val TAB_SALES = "tab_sales"
    const val TAB_WAREHOUSE = "tab_warehouse"
    const val TAB_MORE = "tab_more"

    // Detail screens (pushed on top of tabs)
    const val RECEIVING = "receiving"
    const val DISPATCH = "dispatch"
    const val CHECKOUT = "checkout"
    const val INVOICE_LOOKUP = "invoice_lookup"
    const val CUSTOMERS = "customers"
    const val CREATE_QUOTE = "create_quote"
    const val QUOTE_LIST = "quote_list"
    const val QUOTE_DETAIL = "quote_detail/{id}"
    const val RECORD_PAYMENT = "record_payment"
    const val CREATE_PO = "create_po"
    const val LOOKUP = "lookup"
    const val STOCKTAKE = "stocktake"
    const val STOCK_CHECK = "stock_check"
    const val RETURN = "return"
    const val TRANSFER = "transfer"
    const val INVOICE_LIST = "invoice_list"
    const val PO_LIST = "po_list"
}

data class BottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Routes.TAB_HOME, "首頁", Icons.Filled.House, Icons.Outlined.House),
    BottomTab(Routes.TAB_PURCHASE, "採購", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    BottomTab(Routes.TAB_SALES, "銷售", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    BottomTab(Routes.TAB_WAREHOUSE, "倉庫", Icons.Filled.Warehouse, Icons.Outlined.Warehouse),
    BottomTab(Routes.TAB_MORE, "更多", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

@Composable
fun NavGraph(
    scannerManager: ScannerManager,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainTabShell(
                scannerManager = scannerManager,
                navController = navController
            )
        }

        // Detail screens — full screen overlays
        composable(Routes.RECEIVING) { ReceivingScreen(scannerManager = scannerManager) }
        composable(Routes.DISPATCH) { DispatchScreen(scannerManager = scannerManager) }
        composable(Routes.CHECKOUT) { CheckoutScreen(scannerManager = scannerManager) }
        composable(Routes.INVOICE_LOOKUP) { InvoiceLookupScreen(scannerManager = scannerManager) }
        composable(Routes.CUSTOMERS) { CustomerScreen(scannerManager = scannerManager) }
        composable(Routes.CREATE_QUOTE) { CreateQuoteScreen(scannerManager = scannerManager) }
        composable(Routes.RECORD_PAYMENT) { RecordPaymentScreen(scannerManager = scannerManager) }
        composable(Routes.CREATE_PO) { CreatePoScreen(scannerManager = scannerManager) }
        composable(Routes.LOOKUP) { LookupScreen(scannerManager = scannerManager) }
        composable(Routes.STOCKTAKE) { StocktakeScreen(scannerManager = scannerManager) }
        composable(Routes.STOCK_CHECK) { StockCheckScreen(scannerManager = scannerManager) }
        composable(Routes.RETURN) { ReturnScreen(scannerManager = scannerManager) }
        composable(Routes.TRANSFER) { TransferScreen(scannerManager = scannerManager) }
        composable(Routes.INVOICE_LIST) {
            InvoiceListScreen(onNavigate = { navController.navigate(it) })
        }
        composable(Routes.PO_LIST) {
            PoListScreen(onNavigate = { navController.navigate(it) })
        }
        composable(Routes.QUOTE_LIST) {
            QuoteListScreen(
                scannerManager = scannerManager,
                onNavigateToDetail = { id -> navController.navigate("quote_detail/$id") }
            )
        }
        composable(
            Routes.QUOTE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val quoteId = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
            QuoteDetailScreen(
                quoteId = quoteId,
                scannerManager = scannerManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * iOS 風格底部 Tab Bar 主殼層
 */
@Composable
fun MainTabShell(
    scannerManager: ScannerManager,
    navController: NavHostController
) {
    val tabNavController = rememberNavController()
    val currentBackStack by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                bottomTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != tab.route) {
                                tabNavController.navigate(tab.route) {
                                    popUpTo(tabNavController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = Routes.TAB_HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TAB_HOME) {
                HomeScreen(
                    scannerManager = scannerManager,
                    onNavigate = { route -> navController.navigate(route) },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.TAB_PURCHASE) {
                PurchaseTab(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.TAB_SALES) {
                SalesTab(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.TAB_WAREHOUSE) {
                WarehouseTab(onNavigate = { navController.navigate(it) })
            }
            composable(Routes.TAB_MORE) {
                MoreTab(onNavigate = { navController.navigate(it) })
            }
        }
    }
}
