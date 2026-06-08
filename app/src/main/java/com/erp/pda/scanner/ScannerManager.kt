package com.erp.pda.scanner

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 海康 Hikrobotics PDA 掃描管理器
 *
 * 職責:
 * 1. 啟動/停止掃描服務
 * 2. 配置掃描參數（模式、條碼類型、廣播輸出）
 * 3. 接收掃描結果廣播 → SharedFlow 發射給各頁面
 */
class ScannerManager(private val context: Context) {

    companion object {
        private const val PACKAGE = "com.hikrobotics.pdaservice"
        private const val LAUNCH_RECEIVER = "com.pda.service.broadcast.LaunchReceiver"
        private const val CONTROL_RECEIVER = "com.pda.service.broadcast.ServiceControlReceiver"

        const val ACTION_SCAN_DATA = "com.service.scanner.data"
        const val EXTRA_SCAN_CODE = "ScanCode"
        const val EXTRA_SCAN_CODE_TYPE = "ScanCodeType"
    }

    private val _scanResults = MutableSharedFlow<ScanResult>(extraBufferCapacity = 10)
    val scanResults: SharedFlow<ScanResult> = _scanResults.asSharedFlow()

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_SCAN_DATA) return
            val code = intent.getStringExtra(EXTRA_SCAN_CODE) ?: ""
            val codeType = intent.getStringExtra(EXTRA_SCAN_CODE_TYPE) ?: ""
            _scanResults.tryEmit(ScanResult(code, codeType))
        }
    }

    /** 初始化：啟動掃描服務 + 配置參數 + 註冊廣播接收器 */
    fun initialize() {
        // 1. 啟動掃描服務
        sendToLaunchReceiver("com.service.scanner.start")

        // 2. 等待服務初始化
        Thread.sleep(200)

        // 3. 單次掃描模式
        sendToControlReceiver("com.service.scanner.scan.type", "scanType" to "SINGLE")

        // 4. 鬆開觸發鍵即結束
        sendToControlReceiver("com.service.scanner.scan.end.type", "scanEndType" to "RELEASE")

        // 5. 【關鍵】啟用廣播輸出（預設為 OFF）
        sendToControlReceiver("com.service.scanner.broadcast.output", "broadcastOutput" to true)

        // 6. 設定條碼類型: QR | DM | CODE128 | CODE39 = 8192|16384|2|1 = 24579
        sendToControlReceiver("com.service.scanner.codetype", "scanCodeTypes" to 24579)

        // 7. 註冊掃描結果廣播接收器
        context.registerReceiver(
            scanReceiver,
            IntentFilter(ACTION_SCAN_DATA),
            Context.RECEIVER_EXPORTED
        )
    }

    /** 關閉掃描服務 + 取消註冊接收器 */
    fun shutdown() {
        try {
            context.unregisterReceiver(scanReceiver)
        } catch (_: Exception) {}
        sendToLaunchReceiver("com.service.scanner.stop")
    }

    private fun sendToLaunchReceiver(action: String) {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            setComponent(ComponentName(PACKAGE, LAUNCH_RECEIVER))
        }
        context.sendBroadcast(intent)
    }

    private fun sendToControlReceiver(action: String, vararg extras: Pair<String, Any>) {
        val intent = Intent(action).apply {
            setComponent(ComponentName(PACKAGE, CONTROL_RECEIVER))
            extras.forEach { (key, value) ->
                when (value) {
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                }
            }
        }
        context.sendBroadcast(intent)
    }
}
