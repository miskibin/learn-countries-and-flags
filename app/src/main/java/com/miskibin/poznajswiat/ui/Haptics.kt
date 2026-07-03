package com.miskibin.poznajswiat.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Tactile feedback for quiz answers — a big part of the "game feel". */
class Haptics(private val vibrator: Vibrator?) {

    private fun vibrate(effect: VibrationEffect) {
        runCatching { vibrator?.vibrate(effect) }
    }

    /** Short happy double-tick on a correct answer. */
    fun success() = vibrate(
        VibrationEffect.createWaveform(longArrayOf(0, 25, 70, 35), intArrayOf(0, 160, 0, 220), -1)
    )

    /** Single firm buzz on a wrong answer. */
    fun error() = vibrate(VibrationEffect.createOneShot(160, 190))

    /** Tiny tick, e.g. for a wrong map tap that still leaves tries. */
    fun tick() = vibrate(VibrationEffect.createOneShot(25, 110))

    /** Celebratory triple burst for finishing a session with a great score. */
    fun celebrate() = vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 40, 80, 40, 80, 70),
            intArrayOf(0, 180, 0, 200, 0, 255),
            -1,
        )
    )
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        Haptics(vibrator)
    }
}
