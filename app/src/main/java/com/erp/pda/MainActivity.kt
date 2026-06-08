package com.erp.pda

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.erp.pda.scanner.ScannerManager
import com.erp.pda.ui.navigation.NavGraph
import com.erp.pda.ui.theme.ErpPdaTheme

class MainActivity : ComponentActivity() {

    lateinit var scannerManager: ScannerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化掃描管理器
        scannerManager = ScannerManager(this)
        scannerManager.initialize()

        setContent {
            ErpPdaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(scannerManager = scannerManager)
                }
            }
        }
    }

    override fun onDestroy() {
        scannerManager.shutdown()
        super.onDestroy()
    }
}
