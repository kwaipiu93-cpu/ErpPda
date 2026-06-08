package com.erp.pda.ui.createpo

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
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePoScreen(
    scannerManager: ScannerManager,
    viewModel: CreatePoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSuppliersAndWarehouses() }
    LaunchedEffect(state.step) {
        if (state.step == 1) {
            scannerManager.scanResults.collect { result ->
                ScanFeedback.success()
                viewModel.scanBarcode(result.code)
                viewModel.clearFeedback()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.step == 2) "建立完成" else "建立採購單") },
                navigationIcon = {
                    if (state.step == 1) IconButton(onClick = viewModel::backToSupplier) { Icon(Icons.Filled.ArrowBack, "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TileBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
                2 -> DoneStep(viewModel)
                1 -> ItemsStep(state, viewModel)
                else -> SupplierStep(state, viewModel)
            }
        }
    }
}

@Composable
fun SupplierStep(state: CreatePoUiState, vm: CreatePoViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(value = state.supplierSearch, onValueChange = vm::searchSuppliers, label = { Text("搜尋供應商") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("選擇供應商", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(state.suppliers.filter { state.supplierSearch.isBlank() || it.companyNameEn.contains(state.supplierSearch, true) || it.companyNameZh.contains(state.supplierSearch, true) }) { s ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { vm.selectSupplier(s) },
                    colors = CardDefaults.cardColors(containerColor = if (state.selectedSupplier?.id == s.id) TileBlue.copy(alpha = 0.1f) else Color.Transparent)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.companyNameZh.ifBlank { s.companyNameEn }, fontWeight = FontWeight.Bold)
                            Text("${s.supplierType} | ${s.currencyCode} | ${s.contactPerson}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.selectedSupplier?.id == s.id) Icon(Icons.Filled.CheckCircle, "已選", tint = TileBlue)
                    }
                }
            }
        }
        Button(onClick = vm::goToItems, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = state.selectedSupplier != null,
            colors = ButtonDefaults.buttonColors(containerColor = TileBlue)) { Text("下一步: 加入商品") }
    }
}

@Composable
fun ItemsStep(state: CreatePoUiState, vm: CreatePoViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        state.selectedSupplier?.let { s ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = TileBlue.copy(alpha = 0.1f))) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.companyNameZh.ifBlank { s.companyNameEn }, fontWeight = FontWeight.Bold)
                    Text("${s.supplierType} | ${s.currencyCode}")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.productSearch, onValueChange = vm::searchProducts, label = { Text("掃描條碼 或 搜尋商品") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Text("採購明細 (${state.quoteItems.size} 項)", fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(state.quoteItems) { index, item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    if (state.editIndex == index) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.product.skuCode, fontWeight = FontWeight.Bold)
                            Row {
                                OutlinedTextField(state.editQty, vm::updateEditQty, label = { Text("數量") }, modifier = Modifier.weight(1f), singleLine = true)
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(state.editPrice, vm::updateEditPrice, label = { Text("外幣單價") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = vm::cancelEdit) { Text("取消") }
                                Button(onClick = vm::saveEdit) { Text("儲存", color = Color.White) }
                            }
                        }
                    } else {
                        Row(Modifier.padding(12.dp).clickable { vm.startEdit(index) }, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.product.skuCode, fontWeight = FontWeight.Bold)
                                Text(item.product.nameZh, style = MaterialTheme.typography.bodySmall)
                                Text("${item.qty} x ${item.unitPrice} = ${"%.2f".format(item.lineTotal)}", style = MaterialTheme.typography.labelMedium, color = TileBlue)
                            }
                            Row {
                                IconButton(onClick = { vm.adjustQty(index, -1) }, Modifier.size(32.dp)) { Icon(Icons.Filled.Remove, "減", Modifier.size(16.dp)) }
                                Text("${item.qty}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { vm.adjustQty(index, 1) }, Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "加", Modifier.size(16.dp)) }
                            }
                            IconButton(onClick = { vm.removeItem(index) }) { Icon(Icons.Filled.Delete, "刪除", tint = Error) }
                        }
                    }
                }
            }
            // Search results
            if (state.productResults.isNotEmpty() && state.quoteItems.isEmpty()) {
                item { Text("搜尋結果:", Modifier.padding(8.dp), fontWeight = FontWeight.Bold) }
                items(state.productResults) { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { vm.addToPo(p) }) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f)) { Text(p.skuCode, fontWeight = FontWeight.Bold); Text(p.nameZh, style = MaterialTheme.typography.bodySmall) }
                            Icon(Icons.Filled.AddCircle, "加入", tint = TileBlue)
                        }
                    }
                }
            }
        }

        // Warehouse + Notes + Submit
        OutlinedTextField(value = state.notes, onValueChange = vm::updateNotes, label = { Text("備註 (選填)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.warehouses.take(3).forEach { wh ->
                FilterChip(selected = state.selectedWarehouseId == wh.id, onClick = { vm.selectWarehouse(wh.id) }, label = { Text(wh.nameZh) }, modifier = Modifier.weight(1f))
            }
        }
        Button(
            onClick = vm::submitPo, modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isLoading && state.quoteItems.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = TileBlue)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
            else Text("建立採購單 (${state.quoteItems.size} 項)")
        }
    }
}

@Composable
fun DoneStep(vm: CreatePoViewModel) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.CheckCircle, null, Modifier.size(80.dp), tint = Success)
        Spacer(Modifier.height(24.dp))
        Text("採購單已建立！", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = vm::newPo, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = TileBlue)) { Text("建立新採購單") }
    }
}
