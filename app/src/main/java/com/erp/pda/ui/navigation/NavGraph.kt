package com.erp.pda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.checkout.CheckoutScreen
import com.erp.pda.ui.createpo.CreatePoScreen
import com.erp.pda.ui.createquote.CreateQuoteScreen
import com.erp.pda.ui.customers.CustomerScreen
import com.erp.pda.ui.dispatch.DispatchScreen
import com.erp.pda.ui.home.HomeScreen
import com.erp.pda.ui.invoicelookup.InvoiceLookupScreen
import com.erp.pda.ui.login.LoginScreen
import com.erp.pda.ui.lookup.LookupScreen
import com.erp.pda.ui.receiving.ReceivingScreen
import com.erp.pda.ui.recordpayment.RecordPaymentScreen
import com.erp.pda.ui.returns.ReturnScreen
import com.erp.pda.ui.stockcheck.StockCheckScreen
import com.erp.pda.ui.stocktake.StocktakeScreen
import com.erp.pda.ui.transfer.TransferScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val RECEIVING = "receiving"
    const val DISPATCH = "dispatch"
    const val CHECKOUT = "checkout"
    const val INVOICE_LOOKUP = "invoice_lookup"
    const val CUSTOMERS = "customers"
    const val CREATE_QUOTE = "create_quote"
    const val RECORD_PAYMENT = "record_payment"
    const val CREATE_PO = "create_po"
    const val LOOKUP = "lookup"
    const val STOCKTAKE = "stocktake"
    const val STOCK_CHECK = "stock_check"
    const val RETURN = "return"
    const val TRANSFER = "transfer"
}

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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                scannerManager = scannerManager,
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RECEIVING) {
            ReceivingScreen(scannerManager = scannerManager)
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(scannerManager = scannerManager)
        }

        composable(Routes.INVOICE_LOOKUP) {
            InvoiceLookupScreen(scannerManager = scannerManager)
        }

        composable(Routes.CUSTOMERS) {
            CustomerScreen(scannerManager = scannerManager)
        }

        composable(Routes.CREATE_QUOTE) {
            CreateQuoteScreen(scannerManager = scannerManager)
        }

        composable(Routes.RECORD_PAYMENT) {
            RecordPaymentScreen(scannerManager = scannerManager)
        }

        composable(Routes.CREATE_PO) {
            CreatePoScreen(scannerManager = scannerManager)
        }

        composable(Routes.DISPATCH) {
            DispatchScreen(scannerManager = scannerManager)
        }

        composable(Routes.LOOKUP) {
            LookupScreen(scannerManager = scannerManager)
        }

        composable(Routes.STOCKTAKE) {
            StocktakeScreen(scannerManager = scannerManager)
        }

        composable(Routes.STOCK_CHECK) {
            StockCheckScreen(scannerManager = scannerManager)
        }

        composable(Routes.RETURN) {
            ReturnScreen(scannerManager = scannerManager)
        }

        composable(Routes.TRANSFER) {
            TransferScreen(scannerManager = scannerManager)
        }
    }
}
