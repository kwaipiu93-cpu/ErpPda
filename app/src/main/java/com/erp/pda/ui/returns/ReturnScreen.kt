package com.erp.pda.ui.returns

import androidx.compose.foundation.clickable
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
import com.erp.pda.data.model.CreditNote
import com.erp.pda.ui.theme.Primary
import com.erp.pda.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnScreen(
    scannerManager: com.erp.pda.scanner.ScannerManager,
    viewModel: ReturnViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCreditNotes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("退貨驗收") },
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
                    items(state.creditNotes) { cn ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(cn.cnNumber, fontWeight = FontWeight.Bold)
                                    Text("${cn.customerName} | ${cn.creditType}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.confirmReturn(cn) }) {
                                    Icon(Icons.Filled.Check, "確認退貨", tint = Success)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
