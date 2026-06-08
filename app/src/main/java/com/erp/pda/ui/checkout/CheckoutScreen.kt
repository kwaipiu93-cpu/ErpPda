package com.erp.pda.ui.checkout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.data.model.CustomerSummary
import com.erp.pda.data.model.Product
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    scannerManager: ScannerManager,
    viewModel: CheckoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWarehouses()
        scannerManager.scanResults.collect { result ->
            when (state.step) {
                CheckoutStep.CART -> {
                    // Check if scanning for S/N serial or product barcode
                    val currentItem = state.cartItems.getOrNull(state.selectedCartIndex)
                    if (currentItem != null && currentItem.product.isSerialTracked && !currentItem.isSerialComplete) {
                        viewModel.scanSerialForCart(result.code)
                    } else {
                        viewModel.scanBarcode(result.code)
                    }
                    ScanFeedback.success()
                    viewModel.clearFeedback()
                }
                else -> {
                    ScanFeedback.success()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (state.step) {
                        CheckoutStep.CART -> "快速結帳 - 購物車"
                        CheckoutStep.CUSTOMER -> "選擇客戶"
                        CheckoutStep.PAYMENT -> "確認付款"
                        CheckoutStep.DONE -> "結帳完成 ✅"
                    })
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TileCyan, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.feedback?.let { msg ->
                Surface(Modifier.fillMaxWidth(), color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                    Text(msg, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            when (state.step) {
                CheckoutStep.DONE -> DoneStep(state, viewModel)
                else -> StepIndicator(state.step)
            }

            when (state.step) {
                CheckoutStep.CART -> CartStep(state, viewModel)
                CheckoutStep.CUSTOMER -> CustomerStep(state, viewModel)
                CheckoutStep.PAYMENT -> PaymentStep(state, viewModel)
                CheckoutStep.DONE -> {} // handled above
            }
        }
    }
}

@Composable
fun StepIndicator(step: CheckoutStep) {
    val steps = listOf("購物車" to CheckoutStep.CART, "客戶" to CheckoutStep.CUSTOMER, "付款" to CheckoutStep.PAYMENT)
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        steps.forEach { (label, s) ->
            val active = step.ordinal >= s.ordinal
            val current = step == s
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (current) TileCyan else if (active) TileCyan.copy(alpha = 0.3f) else Color.LightGray,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${s.ordinal + 1}",
                            color = if (active) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (active) TileCyan else Color.Gray)
            }
        }
    }
}

@Composable
fun CartStep(state: CheckoutUiState, vm: CheckoutViewModel) {
    Column(Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::searchProducts,
            label = { Text("掃描條碼 或 搜尋商品") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        // Cart items
        val totalItems = state.cartItems.sumOf { it.qty }
        Text("購物車 ($totalItems 件)", Modifier.padding(12.dp), fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)

        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(state.cartItems) { index, item ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.product.skuCode, fontWeight = FontWeight.Bold)
                            Text(item.product.nameZh, style = MaterialTheme.typography.bodySmall)
                            if (item.product.isSerialTracked) {
                                Text("S/N: ${item.scannedSerials.size}/${item.qty}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isSerialComplete) Success else Error)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.adjustQty(index, -1) }, Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Remove, "減", Modifier.size(18.dp))
                            }
                            Text("${item.qty}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { vm.adjustQty(index, 1) }, Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Add, "加", Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { vm.removeCartItem(index) }) {
                            Icon(Icons.Filled.Delete, "刪除", tint = Error)
                        }
                    }
                }
            }

            // Search results
            if (state.searchResults.isNotEmpty() && state.cartItems.isEmpty()) {
                item { Text("搜尋結果:", Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
                items(state.searchResults) { product ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clickable { vm.addToCart(product) }) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(product.skuCode, fontWeight = FontWeight.Bold)
                                Text(product.nameZh, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Filled.AddCircle, "加入", tint = Success)
                        }
                    }
                }
            }
        }

        // Bottom button
        if (state.cartItems.isNotEmpty()) {
            Button(
                onClick = vm::goToCustomer,
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TileCyan)
            ) {
                Text("下一步: 選擇客戶 ($totalItems 件)")
            }
        }
    }
}

@Composable
fun CustomerStep(state: CheckoutUiState, vm: CheckoutViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = state.customerSearch,
            onValueChange = vm::searchCustomers,
            label = { Text("搜尋 B2C 客戶") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(state.customerResults) { c ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { vm.selectCustomer(c) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.selectedCustomer?.id == c.id) TileCyan.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.companyNameZh.ifBlank { c.companyNameEn }, fontWeight = FontWeight.Bold)
                            Text("${c.customerType} | ${c.contactPhone}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.selectedCustomer?.id == c.id) Icon(Icons.Filled.CheckCircle, "已選", tint = Success)
                    }
                }
                HorizontalDivider()
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            OutlinedButton(onClick = vm::backToCart, modifier = Modifier.weight(1f)) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = vm::goToPayment,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = state.selectedCustomer != null,
                colors = ButtonDefaults.buttonColors(containerColor = TileCyan)
            ) { Text("下一步: 付款") }
        }
    }
}

@Composable
fun PaymentStep(state: CheckoutUiState, vm: CheckoutViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        state.selectedCustomer?.let { c ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TileCyan.copy(alpha = 0.1f))) {
                Column(Modifier.padding(12.dp)) {
                    Text("客戶: ${c.companyNameZh.ifBlank { c.companyNameEn }}", fontWeight = FontWeight.Bold)
                    Text("${state.cartItems.size} 項商品, 共 ${state.cartItems.sumOf { it.qty }} 件")
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Text("付款方式", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("FPS" to "轉數快", "Cash" to "現金", "Bank_Transfer" to "銀行轉帳").forEach { (method, label) ->
                FilterChip(
                    selected = state.paymentMethod == method,
                    onClick = { vm.setPaymentMethod(method) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.referenceNumber,
            onValueChange = vm::setReferenceNumber,
            label = { Text("參考編號 (選填)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Text("出貨倉庫", fontWeight = FontWeight.Bold)
        state.warehouses.forEach { wh ->
            Row(Modifier.fillMaxWidth().clickable { vm.selectWarehouse(wh.id) }.padding(8.dp)) {
                RadioButton(selected = state.selectedWarehouseId == wh.id, onClick = { vm.selectWarehouse(wh.id) })
                Text("${wh.nameZh} (${wh.nameEn})")
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            OutlinedButton(onClick = vm::backToCustomer, modifier = Modifier.weight(1f)) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = vm::submitCheckout,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = TileCyan)
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                else Text("確認結帳")
            }
        }
    }
}

@Composable
fun DoneStep(state: CheckoutUiState, vm: CheckoutViewModel) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Success)
        Spacer(Modifier.height(24.dp))
        Text("結帳完成！", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("發票號碼", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(state.resultInvoice ?: "--", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = TileCyan)
                Spacer(Modifier.height(8.dp))
                Text("金額: HKD ${"%.2f".format(state.resultTotal)}", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = vm::newTransaction,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TileCyan)
        ) {
            Text("新交易")
        }
    }
}
