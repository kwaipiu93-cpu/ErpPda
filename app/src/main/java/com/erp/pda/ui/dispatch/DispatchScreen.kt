package com.erp.pda.ui.dispatch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.data.model.DeliveryNote
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*
import com.erp.pda.ui.components.IosTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    scannerManager: ScannerManager,
    viewModel: DispatchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            if (state.selectedDN != null) {
                val ok = viewModel.scanSerial(result.code)
                if (ok) ScanFeedback.success() else ScanFeedback.error(ErpApplication.instance)
                viewModel.clearFeedback()
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadDeliveryNotes() }

    Scaffold(
        topBar = {
            IosTopBar(
                title = if (state.selectedDN != null) "出貨確認" else "選擇出貨單",
                onBack = if (state.selectedDN != null) viewModel::backToList else null
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            state.lastFeedback?.let { msg ->
                Surface(Modifier.fillMaxWidth(), color = if (state.feedbackIsError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                    Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (state.selectedDN == null) {
                if (state.isLoadingList) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.deliveryNotes) { dn ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { viewModel.selectDN(dn) }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(dn.dnNumber, fontWeight = FontWeight.Bold)
                                    Text("${dn.invoiceNumber} | ${dn.customerName}", style = MaterialTheme.typography.bodySmall)
                                }
                                AssistChip(onClick = {}, label = { Text(dn.fsmStatus) })
                            }
                        }
                    }
                }
            } else {
                if (state.isLoadingDetail) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else {
                    val dn = state.selectedDN!!
                    Column(Modifier.fillMaxSize()) {
                        Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = TileGreen.copy(alpha = 0.1f))) {
                            Column(Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(dn.dnNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(dn.fsmStatus) })
                                }
                                Text("客戶: ${dn.customerName}", style = MaterialTheme.typography.bodyMedium)
                                Text("${state.items.size} 項商品待出貨", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        state.items.getOrNull(state.selectedItemIndex)?.let { item ->
                            Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp).background(Color(0xFFFFF8E1))) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("🔍 ${item.dnItem.skuCode}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(item.dnItem.productName, style = MaterialTheme.typography.bodyMedium)
                                    Text("已掃: ${item.scannedSerials.size} / ${item.remaining}", fontWeight = FontWeight.Bold, color = if (item.isComplete) Success else Primary)
                                    item.scannedSerials.forEach { sn -> Text("✓ $sn", style = MaterialTheme.typography.bodySmall, color = Success) }
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))
                        LazyColumn(Modifier.weight(1.5f)) {
                            itemsIndexed(state.items) { index, item ->
                                Surface(Modifier.fillMaxWidth().clickable { viewModel.selectItem(index) }, color = if (index == state.selectedItemIndex) Primary.copy(alpha = 0.1f) else Color.Transparent) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.dnItem.skuCode, fontWeight = FontWeight.Bold)
                                            Text("${item.scannedSerials.size}/${item.remaining}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (item.isComplete) Text("✅")
                                    }
                                }
                                HorizontalDivider()
                            }
                        }

                        Button(
                            onClick = viewModel::submitDispatch,
                            enabled = !state.isSubmitting && state.items.all { it.isComplete },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
                        ) {
                            if (state.isSubmitting) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                            else Text("確認出貨")
                        }
                    }
                }
            }
        }
    }
}
