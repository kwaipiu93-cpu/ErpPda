package com.erp.pda.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 掃描音效 + 震動反饋
 *
 * 規範參照 ERP SPEC V3.5 §5.3:
 * - 成功: 短促「叮」(TONE_PROP_ACK, 150ms), 無震動
 * - S/N 重複: 長鳴 (TONE_PROP_NACK, 800ms), 震動 800ms
 * - S/N 不屬本單: 長鳴 (TONE_PROP_NACK, 800ms), 震動 800ms
 * - 倉庫凍結: 長鳴 (TONE_PROP_NACK, 800ms), 震動 800ms
 * - 無法識別: 三短(TONE_PROP_NACK 3次), 無震動
 */
object ScanFeedback {

    private var toneGenerator: ToneGenerator? = null

    fun init() {
        toneGenerator?.release()
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    /** 成功掃描 */
    fun success() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    /** 錯誤掃描（含震動） */
    fun error(context: Context) {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 500)
        vibrate(context, 800)
    }

    /** 重複 S/N */
    fun duplicate(context: Context) {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 800)
        vibrate(context, 800)
    }

    /** 無法識別條碼 */
    fun unrecognized() {
        for (i in 1..3) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 100)
            Thread.sleep(150)
        }
    }

    private fun vibrate(context: Context, durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
