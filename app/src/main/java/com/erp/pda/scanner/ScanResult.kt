package com.erp.pda.scanner

data class ScanResult(
    val code: String,
    val codeType: String = ""
) {
    /** 判斷是否為 S/N 碼（長度 ≥ 12 且含字母） */
    val isSerialNumber: Boolean
        get() = code.length >= 12 && code.any { it.isLetter() }

    /** 判斷是否為整箱條碼（結尾 -CS 或 8-10 位純數字） */
    val isCartonBarcode: Boolean
        get() = code.endsWith("-CS") || (code.length in 8..10 && code.all { it.isDigit() })

    /** 判斷是否為單品條碼 */
    val isPieceBarcode: Boolean
        get() = !isSerialNumber && !isCartonBarcode
}
