package com.erp.pda.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.ErpApplication
import com.erp.pda.data.model.CustomerDetail
import com.erp.pda.data.model.CustomerSummary
import com.erp.pda.feedback.ScanFeedback
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    scannerManager: ScannerManager,
    viewModel: CustomerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        scannerManager.scanResults.collect { result ->
            ScanFeedback.success()
            viewModel.setSearchAndSearch(result.code)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("客戶管理")
                },
                navigationIcon = {
                    if (state.isViewingDetail) {
                        IconButton(onClick = viewModel::backToList) {
                            Icon(Icons.Filled.ArrowBack, "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TileTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (state.isViewingDetail) {
                CustomerDetailStep(state, viewModel)
            } else {
                CustomerListStep(state, viewModel)
            }
        }
    }
}

@Composable
private fun CustomerListStep(state: CustomerUiState, viewModel: CustomerViewModel) {
    // Search input
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text("搜尋客戶 (名稱 或 掃描條碼)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = viewModel::search, enabled = !state.isLoading) {
            Icon(Icons.Filled.Search, "搜尋")
        }
    }

    Spacer(Modifier.height(12.dp))

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (state.error != null) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                state.error!!,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    } else if (state.displayedCustomers.isNotEmpty()) {
        Text(
            if (state.searchQuery.isNotBlank()) "搜尋結果 (${state.displayedCustomers.size})" else "全部客戶 (${state.displayedCustomers.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TileTeal
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.displayedCustomers, key = { it.id }) { customer ->
                CustomerCard(customer = customer, onClick = { viewModel.viewDetail(customer.id) })
            }
        }
    } else if (!state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.searchQuery.isNotBlank()) {
                    Text("無匹配結果", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("暫無客戶資料", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("請在 Web ERP 後台新增客戶", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(customer: CustomerSummary, onClick: () -> Unit) {
    val hasOutstanding = ((customer.outstandingHkd?.toDoubleOrNull() ?: 0.0) > 0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Business,
                contentDescription = null,
                tint = TileTeal,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.companyNameZh.ifBlank { customer.companyNameEn },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = customer.contactPerson?.ifBlank { "--" } ?: "--",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.Phone, null, Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = customer.contactPhone?.ifBlank { "--" } ?: "--",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "未結餘額",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "HKD ${customer.outstandingHkd}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasOutstanding) Error else Success
                        )
                    }
                    Column {
                        Text(
                            text = "信用額度",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "HKD ${customer.creditLimitHkd}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TileTeal
                        )
                    }
                }
            }
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CustomerDetailStep(state: CustomerUiState, viewModel: CustomerViewModel) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val customer = state.selectedCustomer ?: return

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Company Info ──
        item {
            SectionCard(title = "公司資訊", accentColor = TileTeal) {
                DetailRow("公司名稱 (中)", customer.companyNameZh.ifBlank { "--" } ?: "--")
                DetailRow("公司名稱 (英)", customer.companyNameEn.ifBlank { "--" } ?: "--")
                DetailRow("客戶類型", customer.customerType.ifBlank { "--" } ?: "--")
                DetailRow(
                    "商業登記號碼 (BR)",
                    customer.brNumber ?: "--",
                    valueColor = if (customer.brNumber != null) Color.Unspecified else Color.Gray
                )
                DetailRow(
                    "狀態",
                    if (customer.isActive) "有效" else "停用",
                    valueColor = if (customer.isActive) Success else Error
                )
            }
        }

        // ── Contact Info ──
        item {
            SectionCard(title = "聯絡資訊", accentColor = TileBlue) {
                DetailRow("聯絡人", customer.contactPerson?.ifBlank { "--" } ?: "--")
                DetailRow("電話", customer.contactPhone?.ifBlank { "--" } ?: "--")
                DetailRow("電郵", customer.contactEmail?.ifBlank { "--" } ?: "--")
            }
        }

        // ── Addresses ──
        item {
            SectionCard(title = "地址", accentColor = TileGreen) {
                DetailRow("帳單地址", customer.billingAddress?.ifBlank { "--" } ?: "--")
                DetailRow("送貨地址", customer.shippingAddress?.ifBlank { "--" } ?: "--")
            }
        }

        // ── Credit & Balance ──
        item {
            SectionCard(title = "信用與結餘", accentColor = TileOrange) {
                DetailRow("信用期", "${customer.creditTermDays} 天")
                DetailRow(
                    "信用額度",
                    "HKD ${customer.creditLimitHkd}",
                    valueColor = TileTeal
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                val hasOutstanding = ((customer.outstandingHkd?.toDoubleOrNull() ?: 0.0) > 0.0)
                DetailRow(
                    "未結餘額",
                    "HKD ${customer.outstandingHkd}",
                    valueColor = if (hasOutstanding) Error else Success,
                    valueWeight = FontWeight.Bold
                )
                if (hasOutstanding) {
                    Text(
                        text = "⚠ 此客戶尚有未結餘額",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionCard(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    valueWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.width(130.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueWeight,
            color = valueColor
        )
    }
}
