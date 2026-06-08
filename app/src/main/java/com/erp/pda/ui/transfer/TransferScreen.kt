package com.erp.pda.ui.transfer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erp.pda.data.model.StockTransfer
import com.erp.pda.ui.theme.Primary
import com.erp.pda.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    scannerManager: com.erp.pda.scanner.ScannerManager,
    viewModel: TransferViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadTransfers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跨倉調撥") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.feedback?.let {
                Surface(Modifier.fillMaxWidth(), color = if (state.feedbackError) MaterialTheme.colorScheme.errorContainer else Success.copy(alpha = 0.2f)) {
                    Text(it, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.transfers) { st ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(st.stNumber, fontWeight = FontWeight.Bold)
                                    Text("${st.fromWarehouseName} → ${st.toWarehouseName}", style = MaterialTheme.typography.bodySmall)
                                    Text(st.fsmStatus, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { viewModel.receiveTransfer(st) }) {
                                    Icon(Icons.Filled.Check, "確認接收", tint = Success)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
